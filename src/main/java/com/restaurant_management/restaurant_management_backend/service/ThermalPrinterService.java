package com.restaurant_management.restaurant_management_backend.service;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.nio.charset.Charset;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.restaurant_management.restaurant_management_backend.dto.OrderDTO;
import com.restaurant_management.restaurant_management_backend.dto.OrderItemDTO;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class ThermalPrinterService {

  private static final int LINE_WIDTH = 32;
  private static final Charset PRINTER_CHARSET = Charset.forName("IBM850");

  // ESC/POS command bytes
  private static final byte[] ESC_INIT        = { 0x1B, 0x40 };
  private static final byte[] FS_EXIT_CHINESE = { 0x1C, 0x2E };        // FS . — desactiva modo caracteres chinos
  private static final byte[] ESC_CODEPAGE    = { 0x1B, 0x74, 0x02 };  // PC850 (Multilingual Latin-1)
  private static final byte[] ESC_ALIGN_LEFT  = { 0x1B, 0x61, 0x00 };
  private static final byte[] ESC_ALIGN_CENTER= { 0x1B, 0x61, 0x01 };
  private static final byte[] ESC_BOLD_ON     = { 0x1B, 0x45, 0x01 };
  private static final byte[] ESC_BOLD_OFF    = { 0x1B, 0x45, 0x00 };
  private static final byte[] GS_DOUBLE_WIDTH = { 0x1D, 0x21, 0x20 };  // Double width only
  private static final byte[] GS_NORMAL_SIZE  = { 0x1D, 0x21, 0x00 };
  private static final byte[] ESC_FEED        = { 0x1B, 0x64, 0x04 };  // Feed 4 lines
  private static final byte[] GS_CUT          = { 0x1D, 0x56, 0x01 };  // Partial cut
  private static final byte   LF              = 0x0A;

  @Value("${thermal.printer.device:/dev/usb/lp0}")
  private String printerDevice;

  public void printPrecuenta(OrderDTO order) {
    try (OutputStream out = new FileOutputStream(printerDevice)) {
      byte[] receipt = buildReceipt(order);
      out.write(receipt);
      out.flush();
      log.info("Precuenta impresa para orden {}", order.getOrderCode());
    } catch (IOException e) {
      log.error("Error al imprimir en {}: {}", printerDevice, e.getMessage());
      throw new RuntimeException("No se pudo imprimir el ticket: " + e.getMessage(), e);
    }
  }

  private byte[] buildReceipt(OrderDTO order) throws IOException {
    java.io.ByteArrayOutputStream buf = new java.io.ByteArrayOutputStream();

    // Initialize
    buf.write(ESC_INIT);
    buf.write(FS_EXIT_CHINESE);
    buf.write(ESC_CODEPAGE);

    // Header - PRECUENTA (centered, bold)
    buf.write(ESC_ALIGN_CENTER);
    buf.write(ESC_BOLD_ON);
    buf.write(GS_DOUBLE_WIDTH);
    writeLine(buf, "PRECUENTA");
    buf.write(GS_NORMAL_SIZE);
    buf.write(ESC_BOLD_OFF);

    // Separator
    buf.write(ESC_ALIGN_LEFT);
    writeLine(buf, repeat('-', LINE_WIDTH));

    // Mesa and order code
    String mesa = order.getTableNumber() != null ? "Mesa: " + order.getTableNumber() : "Pedido";
    String cod  = "Cod: " + order.getOrderCode();
    writeLine(buf, leftRight(mesa, cod, LINE_WIDTH));

    // Date and time
    DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    writeLine(buf, "Fecha: " + LocalDateTime.now().format(dtf));

    // Separator
    writeLine(buf, repeat('-', LINE_WIDTH));

    // Column header
    buf.write(ESC_BOLD_ON);
    writeLine(buf, formatItemLine("Producto", "Cant", "Total"));
    buf.write(ESC_BOLD_OFF);

    writeLine(buf, repeat('-', LINE_WIDTH));

    // Items grouped by category
    Map<String, List<OrderItemDTO>> byCategory = groupByCategory(order.getItems());
    boolean showCategoryHeaders = byCategory.size() > 1;

    List<Map.Entry<String, List<OrderItemDTO>>> entries = new java.util.ArrayList<>(byCategory.entrySet());
    for (int i = 0; i < entries.size(); i++) {
      Map.Entry<String, List<OrderItemDTO>> entry = entries.get(i);

      // Category header — only when there are multiple categories
      if (showCategoryHeaders) {
        buf.write(ESC_BOLD_ON);
        writeLine(buf, truncate(entry.getKey().toUpperCase(), LINE_WIDTH));
        buf.write(ESC_BOLD_OFF);
      }

      // Items
      for (OrderItemDTO item : entry.getValue()) {
        String name     = truncate(item.getProduct().getName(), 17);
        String qtyStr   = item.getQuantity() + "x";
        String totalStr = "S/" + String.format("%.2f", item.getSubTotal());
        writeLine(buf, formatItemLine(name, qtyStr, totalStr));
      }

      // Blank line between groups (not after the last one)
      if (i < entries.size() - 1) {
        writeLine(buf, "");
      }
    }

    // Separator + TOTAL
    writeLine(buf, repeat('-', LINE_WIDTH));
    buf.write(ESC_BOLD_ON);
    String totalStr = "S/ " + String.format("%.2f", order.getTotal());
    writeLine(buf, leftRight("TOTAL:", totalStr, LINE_WIDTH));
    buf.write(ESC_BOLD_OFF);
    writeLine(buf, repeat('-', LINE_WIDTH));

    // Footer
    buf.write(ESC_ALIGN_CENTER);
    writeLine(buf, "");
    writeLine(buf, "Gracias por su visita");
    writeLine(buf, "");

    // Feed and cut
    buf.write(ESC_FEED);
    buf.write(GS_CUT);

    return buf.toByteArray();
  }

  private void writeLine(java.io.ByteArrayOutputStream buf, String text) throws IOException {
    buf.write(text.getBytes(PRINTER_CHARSET));
    buf.write(LF);
  }

  /**
   * Formats a 3-column item line fitting LINE_WIDTH chars.
   * Left column: name (17 chars), center: qty (4 chars), right: total (rest)
   */
  private String formatItemLine(String name, String qty, String total) {
    int rightWidth = LINE_WIDTH - 17 - 1 - 4 - 1; // remaining for total
    return String.format("%-17s %-4s %s",
        truncate(name, 17),
        qty,
        padLeft(total, rightWidth > 0 ? rightWidth : 8));
  }

  /** Aligns two strings left and right within a fixed width. */
  private String leftRight(String left, String right, int width) {
    int spaces = width - left.length() - right.length();
    if (spaces < 1) spaces = 1;
    return left + repeat(' ', spaces) + right;
  }

  private String padLeft(String s, int width) {
    if (s.length() >= width) return s;
    return repeat(' ', width - s.length()) + s;
  }

  private String truncate(String s, int max) {
    if (s == null) return "";
    if (s.length() <= max) return s;
    return s.substring(0, max - 1) + ".";
  }

  private String repeat(char c, int times) {
    return String.valueOf(c).repeat(Math.max(0, times));
  }

  private Map<String, List<OrderItemDTO>> groupByCategory(List<OrderItemDTO> items) {
    Map<String, List<OrderItemDTO>> result = new LinkedHashMap<>();
    if (items == null) return result;
    for (OrderItemDTO item : items) {
      String cat = item.getProduct() != null && item.getProduct().getCategory() != null
          ? item.getProduct().getCategory().getName()
          : "General";
      result.computeIfAbsent(cat, k -> new java.util.ArrayList<>()).add(item);
    }
    return result;
  }
}

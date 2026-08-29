package com.restaurant_management.restaurant_management_backend.voiceorder;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.anthropic.client.AnthropicClient;
import com.anthropic.core.JsonSchemaLocalValidation;
import com.anthropic.models.messages.JsonOutputFormat;
import com.anthropic.models.messages.Message;
import com.anthropic.models.messages.MessageCreateParams;
import com.anthropic.models.messages.OutputConfig;
import com.anthropic.models.messages.StructuredMessage;
import com.anthropic.models.messages.StructuredMessageCreateParams;
import com.anthropic.models.messages.StructuredTextBlock;
import com.restaurant_management.restaurant_management_backend.menu.products.ProductRepository;
import com.restaurant_management.restaurant_management_backend.menu.products.entity.Product;
import com.restaurant_management.restaurant_management_backend.menu.products.productvariants.ProductVariantRepository;
import com.restaurant_management.restaurant_management_backend.menu.products.productvariants.entity.ProductVariant;
import com.restaurant_management.restaurant_management_backend.voiceorder.dto.VoiceOrderExtraction;

import lombok.RequiredArgsConstructor;

/**
 * Extracts a {@link VoiceOrderExtraction} from a dictated order using Claude structured
 * outputs. Read-only against the menu catalog. Never writes to the database, never calls into
 * the real orders flow — the extraction it returns is a best-effort guess that MUST be
 * validated by {@link VoiceOrderValidator} and confirmed by a human before becoming a real
 * order.
 *
 * <p><b>SDK note — verified against the installed anthropic-java 2.34.0 jar, not just its
 * public docs.</b> The naive {@code MessageCreateParams.builder()....outputConfig(Class)}
 * overload returns a {@code StructuredMessageCreateParams.Builder<T>} that has no way to also
 * set {@code effort} — its class-based {@code outputConfig(Class<T>, JsonSchemaLocalValidation)}
 * always overwrites the whole {@code OutputConfig} with format-only, so it can't be combined
 * with {@code effort} directly.
 *
 * <p>A first attempt used {@code com.anthropic.core.StructuredOutputsKt.outputFormatFromClass}
 * to derive the {@link JsonOutputFormat} directly and hand-build an {@link OutputConfig} with
 * both format and effort — this is what the task brief specified, and it matches every public
 * method signature in that class as seen via {@code javap}. It does NOT compile: {@code javap -v}
 * shows every member of {@code StructuredOutputsKt} carries the {@code ACC_SYNTHETIC} flag, which
 * javac treats as inaccessible from Java source even though the bytecode is public — it is Kotlin
 * implementation-detail code, not a supported Java entry point, despite living in a
 * non-{@code internal} package next to {@code JsonSchemaLocalValidation}.
 *
 * <p>The working alternative kept here: derive the schema through the same class-based overload
 * used above (which is NOT synthetic — confirmed via {@code javap}), extract its generated
 * {@link JsonOutputFormat} back out of {@link StructuredMessageCreateParams#rawParams()}, then
 * build a second, plain {@link OutputConfig} that carries both that format and {@code effort}.
 * The final request is sent as a plain (non-structured) {@code MessageCreateParams}, and the raw
 * {@link Message} response is wrapped in a {@link StructuredMessage} — whose constructor is
 * public — to get the same typed {@code .text()} convenience the class-based overload would
 * have given directly.
 */
@Service
@RequiredArgsConstructor
public class VoiceOrderExtractionService {

  // claude-haiku-4-5: cheapest current model ($1/$5 per MTok vs $5/$25 on Opus 5). A/B tested
  // against Opus 5 on the "causas rellenas" false-match bug (2026-08-29): BOTH models fabricated
  // a number instead of emitting null (productId/selectedPrice/tableNumber) — Opus's fabricated
  // values (364, 720, 400) weren't even closer to real. That rules out "model capability" as the
  // cause; it's the JSON schema forcing non-null on these fields, not a Haiku limitation — so
  // there's no quality reason to pay Opus pricing here. See VoiceOrderExtractionService's
  // schema-nullability gap, tracked as a known open issue below.
  // NOTE: Haiku 4.5 does not support the `effort` parameter (errors if set), unlike Opus 5/4.8/
  // 4.7, Sonnet 5, and Fable 5 — do not add `.effort(...)` back without checking first.
  private static final String MODEL = "claude-haiku-4-5";
  private static final long MAX_TOKENS = 4096L;

  private final AnthropicClient anthropicClient;
  private final ProductRepository productRepository;
  private final ProductVariantRepository productVariantRepository;

  public VoiceOrderExtraction extract(String dictatedText) {
    String catalogContext = buildCatalogContext();
    String systemPrompt = buildSystemPrompt(catalogContext);

    JsonOutputFormat derivedFormat = deriveOutputFormat(systemPrompt, dictatedText);

    OutputConfig outputConfig = OutputConfig.builder()
      .format(derivedFormat)
      .build();

    MessageCreateParams params = MessageCreateParams.builder()
      .model(MODEL)
      .maxTokens(MAX_TOKENS)
      .outputConfig(outputConfig)
      .system(systemPrompt)
      .addUserMessage(dictatedText)
      .build();

    Message rawResponse = anthropicClient.messages().create(params);

    StructuredMessage<VoiceOrderExtraction> response =
      new StructuredMessage<>(VoiceOrderExtraction.class, rawResponse);

    return response.content().stream()
      .flatMap(cb -> cb.text().stream())
      .findFirst()
      .map(StructuredTextBlock::text)
      .orElseThrow(() -> new IllegalStateException("Claude did not return a structured text block"));
  }

  /**
   * Builds a throwaway {@link StructuredMessageCreateParams} purely to let the SDK derive the
   * JSON schema for {@link VoiceOrderExtraction} from its Jackson annotations — no API call is
   * made with these params, only {@code .build()} runs locally.
   */
  private JsonOutputFormat deriveOutputFormat(String systemPrompt, String dictatedText) {
    StructuredMessageCreateParams<VoiceOrderExtraction> probe = MessageCreateParams.builder()
      .model(MODEL)
      .maxTokens(MAX_TOKENS)
      .system(systemPrompt)
      .addUserMessage(dictatedText)
      .outputConfig(VoiceOrderExtraction.class, JsonSchemaLocalValidation.YES)
      .build();

    return probe.rawParams().outputConfig()
      .flatMap(OutputConfig::format)
      .orElseThrow(() -> new IllegalStateException("Expected the class-based outputConfig overload to derive a format"));
  }

  // NOTE: intentionally includes disabled products/variants, marked "[NO DISPONIBLE]". Sending
  // only available items made a real, disabled dish literally invisible to the model — instead
  // of matching it and letting the validator report NOT_AVAILABLE, the model fragmented the
  // dictated phrase into unrelated available dishes it could match. Visibility here doesn't
  // widen what the model is trusted to decide (the validator still has final say on
  // availability) — it just gives it the option to name the right productId in the first place.
  private String buildCatalogContext() {
    List<Product> products = productRepository.findAllWithCategory();

    return products.stream()
      .map(this::renderProductLine)
      .collect(Collectors.joining("\n"));
  }

  private String renderProductLine(Product product) {
    List<ProductVariant> variants = productVariantRepository.findByProductId(product.getId());

    String basePrice = product.getPrice().toPlainString()
      + (Boolean.TRUE.equals(product.getIsAvailable()) ? "" : " [NO DISPONIBLE]");

    String variantPrices = variants.stream()
      .map(v -> v.getPrice().toPlainString() + (Boolean.TRUE.equals(v.getIsAvailable()) ? "" : " [NO DISPONIBLE]"))
      .collect(Collectors.joining(", "));

    String prices = variantPrices.isEmpty() ? basePrice : basePrice + ", " + variantPrices;

    return "%d | %s | %s".formatted(product.getId(), product.getName(), prices);
  }

  private String buildSystemPrompt(String catalogContext) {
    return """
      Eres un asistente que extrae pedidos de restaurante dictados por un mesero peruano, en
      formato de texto libre, y los convierte a un JSON estructurado.

      Reglas de seguridad (no negociables):
      - Extraes lo que se dijo. NUNCA inventas un productId. Un nombre PARECIDO no alcanza —
        si el plato dictado no es CLARAMENTE el mismo plato que uno del catálogo (mismo
        ingrediente principal, misma preparación), deja productId en null en vez de matchear
        al más parecido. Ejemplo: "causa rellena" NO es lo mismo que "Causa Acevichada" del
        catálogo, aunque compartan la palabra "causa" — son platos distintos, y confundirlos
        es un error grave, no un acierto aproximado.
      - Extraes selectedPrice SOLO si el mesero dijo un número de precio en voz alta (por ejemplo
        "de 25", "a 30"). Extraelo exactamente como lo dijo — no lo corrijas para que coincida
        con una variante real, aunque parezca incorrecto. La validación contra el catálogo real
        ocurre por separado, en código determinístico, no por ti.
      - Si en cambio el mesero nombró la variante por su etiqueta en vez de un precio (por
        ejemplo "mediano", "grande", "pescado"), poné esa palabra en variantName — NUNCA la
        conviertas vos a un precio, ni la pongas en selectedPrice.
      - Si el mesero NO dijo ni un precio ni una etiqueta de variante para un ítem (por ejemplo
        "una coca", sin más), dejá selectedPrice y variantName vacíos los dos. NUNCA completes
        selectedPrice con el precio del catálogo ni lo inventes — el código se encarga de resolver
        el precio real cuando corresponda, vos solo extraes lo que efectivamente se dijo.
      - Los productos marcados "[NO DISPONIBLE]" en el catálogo SÍ pueden matchear por nombre —
        no los ignores ni los trates como inexistentes. Extraé su productId igual; la
        disponibilidad la valida el código después, no vos.
      - Si una cantidad viene dicha en una sola frase (por ejemplo "dos trio chaufa de 30"),
        resuélvela en una sola línea con quantity=2 — nunca generes dos líneas separadas de
        quantity=1 para el mismo ítem.
      - Si una nota se refiere a un ítem por posición (por ejemplo "el primero sin ají" o
        "el segundo bien cocido"), asigna esa nota al campo notes del ítem correspondiente
        según el orden en que fueron dictados — no la dejes suelta ni la apliques a otro ítem.
      - Si el audio o texto es ininteligible o de baja confianza, o no puedes identificar
        ningún ítem con confianza razonable, devuelve una lista de items vacía. Nunca adivines
        un producto al azar.

      Catálogo disponible (id | nombre | precios de variantes activas):
      %s
      """.formatted(catalogContext);
  }
}

package com.restaurant_management.restaurant_management_backend;

import java.math.BigDecimal;

import org.aspectj.weaver.bcel.UnwovenClassFile.ChildClass;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import com.restaurant_management.restaurant_management_backend.entity.Category;
import com.restaurant_management.restaurant_management_backend.entity.Product;
import com.restaurant_management.restaurant_management_backend.entity.Role;
import com.restaurant_management.restaurant_management_backend.enums.RoleName;
import com.restaurant_management.restaurant_management_backend.repository.CategoryRepository;
import com.restaurant_management.restaurant_management_backend.repository.ProductRepository;
import com.restaurant_management.restaurant_management_backend.repository.RoleRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner{
  private final RoleRepository roleRepository;
  private final CategoryRepository categoryRepository;
  private final ProductRepository productRepository;

  @Override
  @Transactional
  public void run(String... args) throws Exception {
    if (roleRepository.findAll().isEmpty()) {
      roleRepository.save(new Role(null, RoleName.ADMIN));
      roleRepository.save(new Role(null, RoleName.CASHIER));
      roleRepository.save(new Role(null, RoleName.CHEF));
      roleRepository.save(new Role(null, RoleName.WAITER));
    }

    if (categoryRepository.findAll().isEmpty()) {

      Category trios = new Category(null, "Trios Marinos", null);
      categoryRepository.save(trios);

      productRepository.save(new Product(null, "Arroz c/ marisco, ceviche y chicharrón de pota", BigDecimal.valueOf(20), trios));
      productRepository.save(new Product(null, "Arroz c/ marisco, ceviche y chicharrón de pescado", BigDecimal.valueOf(25), trios));
      productRepository.save(new Product(null, "Arroz c/ chaufa, ceviche y chicharrón de pota", BigDecimal.valueOf(20), trios));
      productRepository.save(new Product(null, "Arroz c/ chaufa, ceviche y chicharrón de pescado", BigDecimal.valueOf(25), trios));


      Category duos = new Category(null, "Dúos Marinos", null);
      categoryRepository.save(duos);

      productRepository.save(new Product(null, "Ceviche c/ chicharrón de pota", BigDecimal.valueOf(25), duos));
      productRepository.save(new Product(null, "Ceviche c/ chicharrón de pescado", BigDecimal.valueOf(30), duos));
      productRepository.save(new Product(null, "Ceviche c/ arroz con mariscos", BigDecimal.valueOf(25), duos));
      productRepository.save(new Product(null, "Ceviche c/ arroz chaufa", BigDecimal.valueOf(25), duos));
      productRepository.save(new Product(null, "Arroz c/ mariscos y chicharrón de pota", BigDecimal.valueOf(25), duos));
      productRepository.save(new Product(null, "Arroz c/ mariscos y chicharrón de pescado", BigDecimal.valueOf(30), duos));
      productRepository.save(new Product(null, "Arroz chaufa y chicharrón de pota", BigDecimal.valueOf(25), duos));
      productRepository.save(new Product(null, "Arroz chaufa y chicharrón de pescado", BigDecimal.valueOf(30), duos));


      Category frituras = new Category(null, "Frituras", null);
      categoryRepository.save(frituras);

      productRepository.save(new Product(null, "Trucha frita c/ arroz, yuca  y ensalada", BigDecimal.valueOf(15), frituras));
      productRepository.save(new Product(null, "Pescado frito c/ arroz, yuca y ensalada", BigDecimal.valueOf(10), frituras));


      Category solos = new Category(null, "Platos Solos", null);
      categoryRepository.save(solos);

      productRepository.save(new Product(null, "Ceviche solo", BigDecimal.valueOf(20), solos));
      productRepository.save(new Product(null, "Leche de Tigre", BigDecimal.valueOf(15), solos));
      productRepository.save(new Product(null, "Arroz con Mariscos", BigDecimal.valueOf(18), solos));
      productRepository.save(new Product(null, "Chaufa de Mariscos", BigDecimal.valueOf(18), solos));
      productRepository.save(new Product(null, "Chaufa de Pescado", BigDecimal.valueOf(25), solos));
      productRepository.save(new Product(null, "Causa Acevichada", BigDecimal.valueOf(15), solos));


      Category sopas = new Category(null, "Sopas", null);
      categoryRepository.save(sopas);

      productRepository.save(new Product(null, "Chilcano Especial", BigDecimal.valueOf(12), sopas));
      productRepository.save(new Product(null, "Sudado", BigDecimal.valueOf(15), sopas));
      productRepository.save(new Product(null, "Parihuela", BigDecimal.valueOf(20), sopas));
      productRepository.save(new Product(null, "Chupe de Langostino", BigDecimal.valueOf(15), sopas));
      productRepository.save(new Product(null, "Chupe de Pescado", BigDecimal.valueOf(15), sopas));
      productRepository.save(new Product(null, "Chupe Acevichado", BigDecimal.valueOf(20), sopas));


      Category chicharrones = new Category(null, "Chicharrones", null);
      categoryRepository.save(chicharrones);

      productRepository.save(new Product(null, "Chicharrón de Pota", BigDecimal.valueOf(20), chicharrones));
      productRepository.save(new Product(null, "Chicharrón de Pescado", BigDecimal.valueOf(30), chicharrones));
      productRepository.save(new Product(null, "Jalea Mixta", BigDecimal.valueOf(30), chicharrones));


      Category fuentes = new Category(null, "Fuentes Marinas", null);
      categoryRepository.save(fuentes);

      productRepository.save(new Product(null, "Ceviche, arroz con mariscos /chaufa y chicharrón de pota / pescado.", BigDecimal.valueOf(65), fuentes));
      productRepository.save(new Product(null, "Ceviche solo o con Chicharrón", BigDecimal.valueOf(65), fuentes));
      productRepository.save(new Product(null, "Arroz con mariscos o chaufa de mariscos  Chicharrón", BigDecimal.valueOf(65), fuentes));


      Category bebidas = new Category(null, "Bebidas", null);
      categoryRepository.save(bebidas);

      productRepository.save(new Product(null, "Gaseosa Personal", BigDecimal.valueOf(2), bebidas));
      productRepository.save(new Product(null, "Agua Personal", BigDecimal.valueOf(2), bebidas));
      productRepository.save(new Product(null, "Gordita", BigDecimal.valueOf(5), bebidas));
      productRepository.save(new Product(null, "1 Lt. Coca / Inca", BigDecimal.valueOf(6.5), bebidas));
      productRepository.save(new Product(null, "1 1/2 Coca / Inca", BigDecimal.valueOf(8.5), bebidas));
      productRepository.save(new Product(null, "3 Lt Coca / Inca", BigDecimal.valueOf(15), bebidas));
      productRepository.save(new Product(null, "Maracuya 1 Lt.", BigDecimal.valueOf(6), bebidas));
      productRepository.save(new Product(null, "Chicha 1 Lt.", BigDecimal.valueOf(6), bebidas));
      productRepository.save(new Product(null, "Cerveza Pilsen", BigDecimal.valueOf(8), bebidas));
      productRepository.save(new Product(null, "Cerveza Negrita", BigDecimal.valueOf(10), bebidas));
      productRepository.save(new Product(null, "Cerveza de Trigo", BigDecimal.valueOf(10), bebidas));

    }

    System.out.println("Data Inicializada...");

  }

}

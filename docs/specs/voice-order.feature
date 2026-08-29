# language: es
Feature: Tomar pedido por voz

  Como mesero, quiero dictar un pedido en voz alta para no tener que
  tocar la carta ítem por ítem en la tablet.

  Principio de diseño (no negociable):
    - La IA solo extrae intención y datos a JSON: productId, selectedPrice,
      quantity, notes. Nunca decide precio, nunca escribe en la DB.
    - Todo lo demás -- validar contra el catálogo real, calcular subtotal,
      crear la orden -- es código determinístico, no IA.
    - El preview generado por la IA SIEMPRE pasa por confirmación humana
      del mesero antes de convertirse en una orden real.
    - El precio base de un producto es una opción tan válida como cualquiera
      de sus variantes -- ninguno es un caso especial del otro (mismo criterio
      que ListProducts.tsx en el frontend).

  Background:
    Dado que el catálogo tiene el producto "Trio marisco" con precio base 25 y variante "Pescado" a 30
    Y el catálogo tiene el producto "Trio chaufa" con precio base 25 y variante "Pescado" a 30
    Y el mesero tiene sesión activa

  # NOTA: el nombre real en catálogo es "Trio marisco", pero el mesero suele decir
  # "trio marino" -- no son la misma palabra. Hoy no hay tabla de alias en el
  # esquema (Product no tiene ese campo); este escenario depende de que el LLM
  # resuelva la cercanía semántica/fonética por sí solo. Si en la práctica falla,
  # hay que agregar un campo de alias editable a Product antes de confiar en esto.
  Scenario: Pedido con dos ítems, precio base y nota por posición
    Cuando el mesero dicta "para la mesa 8, un trio marino de 25, un trio chaufa de 30, el primero sin ají"
    Entonces se genera un preview editable con:
      | producto     | selectedPrice | quantity | notes   |
      | Trio marisco | 25            | 1        | sin ají |
      | Trio chaufa  | 30            | 1        |         |
    Y la mesa del preview es 8
    Y la orden NO se crea hasta que el mesero confirme el preview

  Scenario: El precio dictado no coincide con el precio base ni con ninguna variante
    Cuando el mesero dicta "un trio marisco de 28"
    Entonces el ítem del preview queda marcado "sin resolver"
    Y no se asume ninguna variante por defecto

  # El mesero rara vez dice el precio en voz alta -- lo más natural es nombrar la
  # variante ("mediano", "pescado") o no decir nada. El código resuelve el precio
  # real, nunca la IA: ella solo extrae qué se dijo (un número, una etiqueta, o
  # nada), nunca decide cuánto cuesta.
  Scenario: El mesero nombra la variante por su etiqueta, sin decir el precio
    Cuando el mesero dicta "un ceviche con chicharrón de pota mediano"
    Entonces el ítem del preview resuelve al precio real de la variante "Mediano"
    Y ese precio sale del catálogo, nunca de lo que dijo el mesero

  Scenario: El producto tiene un solo precio y el mesero no dice ninguno
    Dado que "1 Lt. Coca / Inca" no tiene variantes
    Cuando el mesero dicta "una coca de un litro"
    Entonces el ítem del preview resuelve automáticamente al precio base
    Y no hace falta que el mesero diga el precio para un producto sin ambigüedad

  Scenario: Ni precio ni variante dichos, y el producto SÍ tiene variantes
    Dado que "Ceviche c/ chicharrón de pota" tiene variantes "Mediano" y "Grande"
    Cuando el mesero dicta "un ceviche" sin más detalle
    Entonces el ítem del preview queda marcado "sin resolver"
    Y no se asume ninguna variante por defecto -- hay más de un precio posible

  Scenario: El producto dictado no existe en el catálogo
    Cuando el mesero dicta "dos causas rellenas"
    Y "causas rellenas" no matchea ningún producto ni alias conocido
    Entonces el ítem del preview queda marcado "no reconocido"
    Y no se inventa ni se asocia a ningún productId

  # Caso real de catálogo: "Arroz c/ marisco, ceviche y chicharrón de pescado" (id 2)
  # está desactivado -- es uno de los 2 platos que se apagaron al quedarse solo con
  # "Trio marisco"/"Trio chaufa" como nombres cortos.
  Scenario: El producto dictado está desactivado
    Dado que "Arroz c/ marisco, ceviche y chicharrón de pescado" tiene is_available=false
    Cuando el mesero dicta "un arroz con marisco, ceviche y chicharrón de pescado"
    Entonces el ítem del preview queda marcado "no disponible"

  Scenario: Agregar ítems a una orden ya abierta en la misma mesa
    Dado que la mesa 8 ya tiene una orden abierta con "Trio marisco" (selectedPrice 25)
    Cuando el mesero dicta "para la mesa 8, agrega una Inca Kola grande"
    Entonces el nuevo ítem se suma a la orden ya abierta de la mesa 8
    Y no se crea una segunda orden para esa mesa

  Scenario: Cantidad implícita en una sola frase
    Cuando el mesero dicta "dos trio chaufa de 30"
    Entonces el preview tiene una sola línea de "Trio chaufa" con quantity 2
    Y no se generan dos líneas separadas de quantity 1

  Scenario: Transcripción de baja confianza o audio ininteligible
    Cuando el audio dictado no puede transcribirse con confianza suficiente
    Entonces no se genera ningún ítem en el preview
    Y se le pide al mesero repetir el pedido
    Y en ningún caso se adivina un producto al azar

  Scenario: Confirmación humana obligatoria antes de crear la orden
    Dado que el preview fue generado correctamente
    Cuando el mesero cierra la tablet sin tocar "Confirmar"
    Entonces la orden no se crea
    Y ningún ítem queda persistido en la base de datos

  # "Para llevar" tiene dos niveles independientes -- no confundir uno con el otro:
  #   1) TODO el pedido es para llevar y no hay mesa (isTakeawayOrder a nivel pedido).
  #   2) Un ítem puntual es para llevar dentro de un pedido de mesa normal
  #      (isTakeaway a nivel de ese ítem nada más).
  Scenario: Pedido nuevo para llevar, sin mesa
    Cuando el mesero dicta "para llevar, dos trio marisco"
    Entonces se genera un preview con isTakeawayOrder=true
    Y el preview no tiene mesa asociada
    Y al confirmar se crea una orden nueva de tipo TAKEAWAY, sin tableId
    Y esa orden nunca reutiliza una orden para llevar existente -- siempre es una orden nueva

  Scenario: Un ítem puntual de un pedido de mesa es para llevar, el resto no
    Cuando el mesero dicta "para la mesa 8, un trio marisco, y una coca para llevar"
    Entonces se genera un preview con isTakeawayOrder=false y mesa 8
    Y el ítem "Trio marisco" queda con isTakeaway=false
    Y el ítem "coca" queda con isTakeaway=true
    Y al confirmar ambos ítems se agregan a la misma orden de la mesa 8
    Y el ítem "coca" lleva el recargo de para llevar si corresponde según su categoría

  Scenario: Confirmar un pedido para llevar sin ningún ítem válido no crea nada
    Dado que un ítem del pedido para llevar no matchea ningún producto del catálogo
    Cuando el mesero intenta confirmar
    Entonces la confirmación se rechaza
    Y no se crea ninguna orden TAKEAWAY huérfana

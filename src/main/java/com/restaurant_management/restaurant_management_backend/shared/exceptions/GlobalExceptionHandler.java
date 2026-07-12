package com.restaurant_management.restaurant_management_backend.shared.exceptions;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestControllerAdvice
public class GlobalExceptionHandler {

  private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

  @ExceptionHandler(ResourceNotFoundException.class)
  public ResponseEntity<ErrorResponse> handleResourceNotFoundException(ResourceNotFoundException exception) {
    ErrorResponse errorResponse = new ErrorResponse(
      exception.getMessage(), 
      HttpStatus.NOT_FOUND.value(), 
      "Recurso no encontrado"
    );

    return new ResponseEntity<>(errorResponse, HttpStatus.NOT_FOUND);
  }

  @ExceptionHandler(ResourceConflictException.class)
  public ResponseEntity<ErrorResponse> handleResourceConflictException(ResourceConflictException exception) {
    ErrorResponse errorResponse = new ErrorResponse(
      exception.getMessage(), 
      HttpStatus.CONFLICT.value(), 
      "Recurso en conflicto"
    );

    return new ResponseEntity<>(errorResponse, HttpStatus.CONFLICT);
  }

  @ExceptionHandler(BadRequestException.class)
  public ResponseEntity<ErrorResponse> handleBadRequestException(BadRequestException exception) {
    ErrorResponse errorResponse = new ErrorResponse(
      exception.getMessage(), 
      HttpStatus.BAD_REQUEST.value(), 
      "Solicitud incorrecta"
    );

    return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<?> handleValidationsExceptions(MethodArgumentNotValidException exception) {
    Map<String, String> errors = new HashMap<>();
    exception.getBindingResult().getFieldErrors()
      .forEach(error -> errors.put(error.getField(), error.getDefaultMessage()));

    String errorMessage = "Errores de validación en los campos: " 
      + String.join(", ", errors.keySet());

    ErrorResponse errorResponse = new ErrorResponse( 
      errorMessage,
      HttpStatus.BAD_REQUEST.value(),
      " Validación fallida"
    );

    return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
  }

  @ExceptionHandler(HttpMessageNotReadableException.class)
  public ResponseEntity<ErrorResponse> handleHttpMessageNotReadable(HttpMessageNotReadableException exception) {
    String message = exception.getMessage();

    if (message != null && message.contains("TableStatus")) {
      message = "El campo 'status' debe ser uno de los valores: FREE, RESERVED, OCCUPIED.";
    } else if (message != null && message.contains("OrderType")) {
      message = "El campo 'type' debe ser uno de los valores: DINE_IN, TAKEAWAY, DELIVERY";
    } else if (message != null && message.contains("OrderStatus")) {
      message = "El campo 'status' debe ser uno de los valores: CREATED, IN_PROGRESS, READY, PARTIALLY_PAID, PAID, CANCELLED";
    } else {
      message = "Error de formato en la petición. Verifica los datos enviados.";
    }

    ErrorResponse errorResponse = new ErrorResponse(
      message,
      HttpStatus.BAD_REQUEST.value(),
      "JSON mal formado o valor inválido"
    );

    return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
  }

  @ExceptionHandler(UnauthorizedException.class)
  public ResponseEntity<ErrorResponse> handleUnauthorizedException(UnauthorizedException exception) {
    ErrorResponse errorResponse = new ErrorResponse( 
      exception.getMessage(),
      HttpStatus.UNAUTHORIZED.value(),
      "Credenciales inválidas"
    );

    return new ResponseEntity<>(errorResponse, HttpStatus.UNAUTHORIZED);
  }

  @ExceptionHandler(TokenExpiredException.class)
  public ResponseEntity<ErrorResponse> handleTokenExpiredException(TokenExpiredException exception) {
    ErrorResponse errorResponse = new ErrorResponse(
      exception.getMessage(),
      HttpStatus.UNAUTHORIZED.value(),
      "Token expirado"
    );

    return new ResponseEntity<>(errorResponse, HttpStatus.UNAUTHORIZED);
  }

  @ExceptionHandler(InvalidTokenException.class)
  public ResponseEntity<ErrorResponse> handleInvalidTokenException(InvalidTokenException exception) {
    ErrorResponse errorResponse = new ErrorResponse(
      exception.getMessage(),
      HttpStatus.UNAUTHORIZED.value(),
      "Token inválido"
    );

    return new ResponseEntity<>(errorResponse, HttpStatus.UNAUTHORIZED);
  }

  @ExceptionHandler(IllegalStateException.class)
  public ResponseEntity<ErrorResponse> handleIllegalStateException(IllegalStateException exception) {
    ErrorResponse errorResponse = new ErrorResponse(
      exception.getMessage(),
      HttpStatus.CONFLICT.value(),
      "Estado inválido"
    );

    return new ResponseEntity<>(errorResponse, HttpStatus.CONFLICT);
  }

  @ExceptionHandler(IllegalArgumentException.class)
  public ResponseEntity<ErrorResponse> handleIllegalArgumentException(IllegalArgumentException exception) {
    ErrorResponse errorResponse = new ErrorResponse(
      exception.getMessage(),
      HttpStatus.BAD_REQUEST.value(),
      "Argumento inválido"
    );

    return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
  }

  @ExceptionHandler(MethodArgumentTypeMismatchException.class)
  public ResponseEntity<ErrorResponse> handleMethodArgumentTypeMismatch(MethodArgumentTypeMismatchException exception) {
    String message = String.format("El parámetro '%s' tiene un formato inválido. Se esperaba: %s",
        exception.getName(),
        exception.getRequiredType() != null ? exception.getRequiredType().getSimpleName() : "formato válido");

    ErrorResponse errorResponse = new ErrorResponse(
      message,
      HttpStatus.BAD_REQUEST.value(),
      "Argumento inválido"
    );

    return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
  }

  @ExceptionHandler(DataIntegrityViolationException.class)
  public ResponseEntity<ErrorResponse> handleDataIntegrityViolationException(DataIntegrityViolationException exception) {
    log.warn("Violación de integridad de datos", exception);

    ErrorResponse errorResponse = new ErrorResponse(
      "No se pudo completar la operación porque el registro está siendo usado por otro dato del sistema.",
      HttpStatus.CONFLICT.value(),
      "Conflicto de integridad de datos"
    );

    return new ResponseEntity<>(errorResponse, HttpStatus.CONFLICT);
  }

  // Red de seguridad: cualquier excepción no mapeada arriba cae acá en vez de
  // escapar al manejador por defecto de Spring (que no devuelve el formato
  // ErrorResponse y termina mostrando texto crudo/en inglés en el frontend).
  @ExceptionHandler(Exception.class)
  public ResponseEntity<ErrorResponse> handleUnexpectedException(Exception exception) {
    log.error("Excepción no controlada", exception);

    ErrorResponse errorResponse = new ErrorResponse(
      "Ocurrió un error inesperado. Intentá de nuevo o avisá a soporte si el problema persiste.",
      HttpStatus.INTERNAL_SERVER_ERROR.value(),
      "Error interno"
    );

    return new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
  }

}

package com.umc.pediupatrao.controller;

import org.springframework.beans.TypeMismatchException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.format.DateTimeParseException;

@RestControllerAdvice(assignableTypes = {
        PedidoController.class,
        ClienteController.class,
        UsuarioController.class,
        ProdutoController.class,
        AuditoriaController.class,
        DashboardController.class
})
public class RestExceptionHandler {

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<String> handleIllegalArgument(IllegalArgumentException ex) {
        return ResponseEntity.badRequest().body(ex.getMessage());
    }

    @ExceptionHandler({TypeMismatchException.class, DateTimeParseException.class})
    public ResponseEntity<String> handleParametroInvalido(Exception ex) {
        return ResponseEntity.badRequest().body("Parâmetro inválido. Datas devem usar o formato yyyy-MM-dd.");
    }
}

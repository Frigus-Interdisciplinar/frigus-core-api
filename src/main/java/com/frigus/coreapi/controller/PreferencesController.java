package com.frigus.coreapi.controller;
import com.frigus.coreapi.dto.preferences.PreferencesDto;
import com.frigus.coreapi.service.PreferencesService;
import lombok.RequiredArgsConstructor;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/profile/preferences") @RequiredArgsConstructor
public class PreferencesController {
 private final PreferencesService service;
 @GetMapping public PreferencesDto get(){return service.get();}
 @PutMapping public PreferencesDto update(@Valid @RequestBody PreferencesDto dto){return service.update(dto);}
}

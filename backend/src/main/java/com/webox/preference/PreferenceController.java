package com.webox.preference;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.webox.api.ApiException;
import com.webox.auth.CurrentUserService;
import com.webox.model.UserPreference;
import com.webox.repository.UserPreferenceRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Pattern;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/me/preferences")
public class PreferenceController {
    private final CurrentUserService currentUsers;
    private final UserPreferenceRepository preferences;
    private final ObjectMapper objectMapper;

    public record PreferenceRequest(
            List<String> allergens,
            List<String> cuisines,
            @Pattern(regexp = "None|Mild|Medium|Hot|", message = "Choose a valid spice level.") String spiceLevel,
            @Pattern(regexp = "Light|Balanced|Rich|", message = "Choose a valid taste preference.") String tasteIntensity,
            @DecimalMin("0.00") BigDecimal budgetMin,
            @DecimalMin("0.00") BigDecimal budgetMax) {}

    public PreferenceController(CurrentUserService currentUsers, UserPreferenceRepository preferences, ObjectMapper objectMapper) {
        this.currentUsers = currentUsers;
        this.preferences = preferences;
        this.objectMapper = objectMapper;
    }

    @GetMapping
    PreferenceRequest get(Authentication authentication) {
        var user = currentUsers.require(authentication);
        return preferences.findByUserId(user.getId()).map(this::toResponse)
                .orElse(new PreferenceRequest(List.of(), List.of(), "", "", null, null));
    }

    @PutMapping
    @Transactional
    PreferenceRequest update(@Valid @RequestBody PreferenceRequest request, Authentication authentication) {
        var user = currentUsers.require(authentication);
        if (request.budgetMin() != null && request.budgetMax() != null
                && request.budgetMin().compareTo(request.budgetMax()) > 0) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_BUDGET_RANGE",
                    "Budget minimum cannot exceed maximum.");
        }
        var preference = preferences.findByUserId(user.getId()).orElseGet(() -> new UserPreference(user));
        try {
            preference.setAllergensJson(objectMapper.writeValueAsString(request.allergens() == null ? List.of() : request.allergens()));
            preference.setCuisinesJson(objectMapper.writeValueAsString(request.cuisines() == null ? List.of() : request.cuisines()));
        } catch (JsonProcessingException exception) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_PREFERENCE_DATA",
                    "Invalid preference data.");
        }
        preference.setSpiceLevel(request.spiceLevel());
        preference.setTasteIntensity(request.tasteIntensity());
        preference.setBudgetMin(request.budgetMin());
        preference.setBudgetMax(request.budgetMax());
        return toResponse(preferences.save(preference));
    }

    private PreferenceRequest toResponse(UserPreference preference) {
        try {
            return new PreferenceRequest(
                    objectMapper.readValue(preference.getAllergensJson(), new TypeReference<List<String>>() {}),
                    objectMapper.readValue(preference.getCuisinesJson(), new TypeReference<List<String>>() {}),
                    preference.getSpiceLevel(), preference.getTasteIntensity(),
                    preference.getBudgetMin(), preference.getBudgetMax());
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Invalid saved preferences", exception);
        }
    }
}

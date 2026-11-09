package com.demo.idempotency;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@JsonPropertyOrder({"customerId", "productId", "quantity"})
public class OrderRequest {

	@NotBlank
    @Size(max = 64)
    private String customerId;

    @NotBlank
    @Size(max = 80)
    private String productId;

    @Min(1)
    @Max(100)
    private int quantity;
}

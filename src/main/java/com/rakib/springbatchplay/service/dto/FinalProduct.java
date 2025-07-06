package com.rakib.springbatchplay.service.dto;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class FinalProduct implements Serializable {
    private Long id;
    private String name;
    private String type;
}

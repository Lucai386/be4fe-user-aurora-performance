package com.be4fe_user_aurora_performance.dto.org;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrgRowDto {

    private String id;
    private String label;
    private String manager;
    private String role;
    private String head;
    private List<String> staff;
    private String color;
}

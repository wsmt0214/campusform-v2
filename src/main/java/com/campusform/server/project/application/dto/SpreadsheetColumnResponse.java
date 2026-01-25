package com.campusform.server.project.application.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class SpreadsheetColumnResponse {
    private String name;
    private Integer index;
}

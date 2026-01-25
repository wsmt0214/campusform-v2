package com.campusform.server.project.domain.model.sheet;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class SpreadsheetColumn {
    private final String name;
    private final int index;
}

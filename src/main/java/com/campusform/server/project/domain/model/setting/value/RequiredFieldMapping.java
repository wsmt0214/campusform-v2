package com.campusform.server.project.domain.model.setting.value;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 필수 필드 매핑 값 객체
 */
@Getter
@AllArgsConstructor
public class RequiredFieldMapping {

    private final Integer nameIdx;
    private final Integer schoolIdx;
    private final Integer majorIdx;
    private final Integer genderIdx;
    private final Integer phoneIdx;
    private final Integer emailIdx;
    private final Integer positionIdx;
}

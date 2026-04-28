package com.campusform.server.global.loadtest;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import com.campusform.server.project.domain.model.sheet.SpreadsheetColumn;

import lombok.RequiredArgsConstructor;

/**
 * 시트 동기화 성능 측정을 위한 고정 규격 데이터셋 생성기
 *
 * - SpreadsheetReader(Fake)와 DB seed가 같은 데이터 규격을 공유하는 역할
 * - 생성 규칙이 코드로 남아 재현 가능한 실험 증거가 되는 역할
 */
@Component
@Profile("loadtest")
@RequiredArgsConstructor
public class SheetSyncLoadtestDataset {

    public static final String LOADTEST_SHEET_URL = "https://sheet.local/loadtest";

    /**
     * 프로젝트의 RequiredFieldMapping(1~7)과 맞추기 위한 헤더 구성
     * 0번은 timestamp 컬럼으로 간주되며 getSheetHeaders 응답에서 제외되는 대상
     */
    public static final int IDX_TIMESTAMP = 0;
    public static final int IDX_NAME = 1;
    public static final int IDX_EMAIL = 2;
    public static final int IDX_PHONE = 3;
    public static final int IDX_GENDER = 4;
    public static final int IDX_SCHOOL = 5;
    public static final int IDX_MAJOR = 6;
    public static final int IDX_POSITION = 7;
    public static final int BASE_COLUMNS = 8;

    private final SheetSyncLoadtestProperties props;

    public List<SpreadsheetColumn> generateHeaders() {
        List<SpreadsheetColumn> headers = new ArrayList<>();
        headers.add(new SpreadsheetColumn("timestamp", IDX_TIMESTAMP));
        headers.add(new SpreadsheetColumn("name", IDX_NAME));
        headers.add(new SpreadsheetColumn("email", IDX_EMAIL));
        headers.add(new SpreadsheetColumn("phone", IDX_PHONE));
        headers.add(new SpreadsheetColumn("gender", IDX_GENDER));
        headers.add(new SpreadsheetColumn("school", IDX_SCHOOL));
        headers.add(new SpreadsheetColumn("major", IDX_MAJOR));
        headers.add(new SpreadsheetColumn("position", IDX_POSITION));

        for (int i = 0; i < props.getExtraColumns(); i++) {
            int idx = BASE_COLUMNS + i;
            headers.add(new SpreadsheetColumn("extra_q_" + (i + 1), idx));
        }
        return headers;
    }

    public DatasetRows generateRows() {
        int rows = Math.max(0, props.getRows());
        String scenario = props.getScenario() != null ? props.getScenario().trim().toLowerCase() : "mixed";

        int unchangedCount;
        int changedCount;
        int newCount;

        switch (scenario) {
            case "all_new" -> {
                unchangedCount = 0;
                changedCount = 0;
                newCount = rows;
            }
            case "all_unchanged" -> {
                unchangedCount = rows;
                changedCount = 0;
                newCount = 0;
            }
            case "all_changed_base_only", "all_changed_extra" -> {
                unchangedCount = 0;
                changedCount = rows;
                newCount = 0;
            }
            case "mixed" -> {
                unchangedCount = (int) Math.round(rows * props.getUnchangedRatio());
                changedCount = (int) Math.round(rows * props.getChangedRatio());
                newCount = rows - unchangedCount - changedCount;
                if (newCount < 0) {
                    newCount = 0;
                    int sum = unchangedCount + changedCount;
                    if (sum > rows) {
                        changedCount = Math.max(0, rows - unchangedCount);
                    }
                }
            }
            default -> {
                unchangedCount = (int) Math.round(rows * props.getUnchangedRatio());
                changedCount = (int) Math.round(rows * props.getChangedRatio());
                newCount = rows - unchangedCount - changedCount;
                if (newCount < 0) {
                    newCount = 0;
                    int sum = unchangedCount + changedCount;
                    if (sum > rows) {
                        changedCount = Math.max(0, rows - unchangedCount);
                    }
                }
            }
        }

        List<Row> unchanged = new ArrayList<>(unchangedCount);
        List<Row> changed = new ArrayList<>(changedCount);
        List<Row> newly = new ArrayList<>(newCount);

        Random random = new Random(props.getRandomSeed());
        LocalDateTime now = LocalDateTime.now();

        int idx = 1;
        for (int i = 0; i < unchangedCount; i++, idx++) {
            unchanged.add(buildRow(now, idx, Variant.UNCHANGED, random));
        }
        for (int i = 0; i < changedCount; i++, idx++) {
            changed.add(buildRow(now, idx, Variant.CHANGED, random));
        }
        for (int i = 0; i < newCount; i++, idx++) {
            newly.add(buildRow(now, idx, Variant.NEW, random));
        }

        List<Row> all = new ArrayList<>(rows);
        all.addAll(unchanged);
        all.addAll(changed);
        all.addAll(newly);

        return new DatasetRows(all, unchanged, changed, newly);
    }

    private Row buildRow(LocalDateTime now, int seq, Variant variant, Random random) {
        String name = "지원자" + seq;
        String email = "applicant" + seq + "@campusform.local";
        String phone = "010-1000-" + String.format("%04d", seq);
        String gender = (seq % 2 == 0) ? "여" : "남";
        String school = "학교" + (seq % 10);
        String major = "전공" + (seq % 7);
        String position = (seq % 2 == 0) ? "백엔드" : "프론트엔드";

        List<String> extraAnswers = new ArrayList<>(props.getExtraColumns());
        for (int i = 0; i < props.getExtraColumns(); i++) {
            extraAnswers.add("ans_" + (i + 1) + "_" + (random.nextInt(1000)));
        }

        return new Row(now, seq, variant, name, email, phone, gender, school, major, position, extraAnswers);
    }

    public enum Variant {
        UNCHANGED,
        CHANGED,
        NEW
    }

    public record Row(
            LocalDateTime timestamp,
            int seq,
            Variant variant,
            String name,
            String email,
            String phone,
            String gender,
            String school,
            String major,
            String position,
            List<String> extraAnswers
    ) {
        public String[] toSheetColumns(int extraColumns) {
            String[] cols = new String[BASE_COLUMNS + extraColumns];
            cols[IDX_TIMESTAMP] = timestamp != null ? timestamp.toString() : "";
            cols[IDX_NAME] = name;
            cols[IDX_EMAIL] = email;
            cols[IDX_PHONE] = phone;
            cols[IDX_GENDER] = gender;
            cols[IDX_SCHOOL] = school;
            cols[IDX_MAJOR] = major;
            cols[IDX_POSITION] = position;
            for (int i = 0; i < extraColumns; i++) {
                cols[BASE_COLUMNS + i] = (extraAnswers != null && i < extraAnswers.size()) ? extraAnswers.get(i) : "";
            }
            return cols;
        }
    }

    public record DatasetRows(
            List<Row> all,
            List<Row> unchanged,
            List<Row> changed,
            List<Row> newly
    ) {
    }
}


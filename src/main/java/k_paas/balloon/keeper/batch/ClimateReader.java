package k_paas.balloon.keeper.batch;

import static k_paas.balloon.keeper.global.constant.ClimateContants.ARRAY_X_INDEX;
import static k_paas.balloon.keeper.global.constant.ClimateContants.ARRAY_Y_INDEX;
import static k_paas.balloon.keeper.global.constant.ClimateContants.ISOBARIC_ALTITUDE;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import k_paas.balloon.keeper.global.async.ClimateAsyncService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.StepExecutionListener;
import org.springframework.batch.item.ItemReader;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class ClimateReader implements ItemReader<List<UpdateClimateServiceSpec>>, StepExecutionListener {

    private int currentAltitudeIndex = 0;
    private static final int CHUNK_SIZE = 100;
    private boolean isCompleted = false;
    private List<UpdateClimateServiceSpec> buffer = new ArrayList<>();

    private final ClimateAsyncService climateAsyncService;

    public ClimateReader(ClimateAsyncService climateAsyncService) {
        this.climateAsyncService = climateAsyncService;
    }

    /**
     * 새 작업 실행시 이전에 실행했던 Reader 작업 후 변수 대한 초기화 수행
     * @param stepExecution instance of {@link StepExecution}.
     */
    @Override
    public void beforeStep(StepExecution stepExecution) {
        this.currentAltitudeIndex = 0;
        this.isCompleted = false;
        this.buffer.clear();
        log.info("ClimateReader initialized with currentAltitudeIndex: {}", currentAltitudeIndex);
    }

    @Override
    public List<UpdateClimateServiceSpec> read() {
        // 작업이 끝났다면 null 처리
        if (isCompleted) {
            return null;
        }

        List<UpdateClimateServiceSpec> chunk = new ArrayList<>();

        while (chunk.size() < CHUNK_SIZE && !isCompleted) {
            if (buffer.isEmpty()) {
//                if (currentAltitudeIndex >= ISOBARIC_ALTITUDE.length) {
                if (currentAltitudeIndex >= 1) {
                    isCompleted = true;
                    break;
                }
                buffer = processClimateData(currentAltitudeIndex, 0);

                currentAltitudeIndex++;
            }

            int itemsToAdd = Math.min(CHUNK_SIZE - chunk.size(), buffer.size());
            chunk.addAll(buffer.subList(0, itemsToAdd));
            buffer = new ArrayList<>(buffer.subList(itemsToAdd, buffer.size()));
        }

        if (chunk.isEmpty()) {
            isCompleted = true;
            return null;
        }

//        log.info("Returning chunk with size: {}", chunk.size());
        return chunk;
    }

    /**
     * U벡터와 V벡터에 대한 값을 각각 비동기로 수행 후 통합을 수행
     * @param altitude 고도
     * @param predictHour 현재로 부터 예측 시간 (요구 사항 변경으로 인한 0 현재값으로 고정)
     * @return
     */
    private List<UpdateClimateServiceSpec> processClimateData(int altitude, int predictHour) {
        CompletableFuture<String[][]> completableUVectors = sendClimateRequest(2002, altitude, predictHour);
        CompletableFuture<String[][]> completableVVectors = sendClimateRequest(2003, altitude, predictHour);

        List<UpdateClimateServiceSpec> result = CompletableFuture.allOf(completableUVectors, completableVVectors)
                .thenApply(r -> {
                    String[][] uVectorArray = completableUVectors.join();
                    String[][] vVectorArray = completableVVectors.join();
                    List<UpdateClimateServiceSpec> updateClimateServiceSpecs = saveClimateData(altitude, predictHour, uVectorArray, vVectorArray);
                    return updateClimateServiceSpecs;
                }).join();
        return result;
    }

    /**
     * 요청 작업을 비동기 방식으로 바꿔주는 Wrapper 클래스 사용
     * @param parameterIndex
     * @param altitude
     * @param predictHour
     * @return
     */
    private CompletableFuture<String[][]> sendClimateRequest(int parameterIndex, int altitude, int predictHour) {
        return climateAsyncService.sendRequest(
                String.valueOf(parameterIndex),
                String.valueOf(ISOBARIC_ALTITUDE[altitude]),
                String.valueOf(predictHour)
        );
    }

    /**
     * 비동기 요청 수행 후 결과를 병합 시키는 작업 수행
     * @param altitude
     * @param predictHour
     * @param uVectorArray
     * @param vVectorArray
     * @return
     */
    private List<UpdateClimateServiceSpec> saveClimateData(int altitude, int predictHour, String[][] uVectorArray, String[][] vVectorArray) {
        List<UpdateClimateServiceSpec> result = new ArrayList<>();

        // TODO: Y 마지막 인덱스 데이터 null 발생
        for (int y = 0; y < ARRAY_Y_INDEX - 1; y++) {
            for (int x = 0; x < ARRAY_X_INDEX; x++) {
                result.add(createUpdateClimateSpec(y, x, altitude, predictHour, uVectorArray, vVectorArray));
            }
        }
        return result;
    }

    /**
     * 병합 결과 DTO 로 변환 Mapper
     * @param y
     * @param x
     * @param altitude
     * @param predictHour
     * @param uVectorArray
     * @param vVectorArray
     * @return
     */
    private UpdateClimateServiceSpec createUpdateClimateSpec(int y, int x, int altitude, int predictHour, String[][] uVectorArray, String[][] vVectorArray) {
        UpdateClimateServiceSpec spec = UpdateClimateServiceSpec.builder()
                .y(y)
                .x(x)
                .pressure(ISOBARIC_ALTITUDE[altitude])
                .uVector(uVectorArray[y][x])
                .vVector(vVectorArray[y][x])
                .build();

        if (!spec.isValid()) {
            throw new IllegalArgumentException("Invalid Null Field UpdateClimateServiceSpec");
        }

        return spec;
    }

    @Override
    public ExitStatus afterStep(StepExecution stepExecution) {
        // 이후 처리가 필요할 경우 여기서 추가 가능
        return ExitStatus.COMPLETED;
    }
}
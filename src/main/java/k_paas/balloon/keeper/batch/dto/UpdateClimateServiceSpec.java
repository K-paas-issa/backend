package k_paas.balloon.keeper.batch.dto;

import k_paas.balloon.keeper.domain.climate.entity.Climate;
import k_paas.balloon.keeper.domain.climate.entity.Climate.ClimateData;
import lombok.Builder;

import java.util.Arrays;

public record UpdateClimateServiceSpec(
        Integer prespredictHour,
        Integer pressure,
        String[][] UVVector,
        Integer pressureValue,
        Integer predictHour,
        Integer uvVectorIndex
) {

    @Builder
    public UpdateClimateServiceSpec {
    }

    @Override
    public String toString() {
        return "UpdateClimateServiceSpec{" +
                "prespredictHour=" + prespredictHour +
                ", pressure=" + pressure +
                ", UVVector=" + Arrays.toString(UVVector) +
                ", pressureValue=" + pressureValue +
                ", predictHour=" + predictHour +
                ", uvVectorIndex=" + uvVectorIndex +
                '}';
    }

    public Climate toEntity(){
        return Climate.builder()
                .prespredictHour(this.prespredictHour)
                .pressure(this.pressure)
                .climateData(
                        ClimateData.builder()
                                .UVVector(this.UVVector)
                                .pressure(this.pressureValue)
                                .predictHour(this.predictHour)
                        .build())
                .build();
    }
}

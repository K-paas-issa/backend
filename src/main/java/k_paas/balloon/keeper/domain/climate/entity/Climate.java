package k_paas.balloon.keeper.domain.climate.entity;

import jakarta.persistence.Id;
import lombok.Builder;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collation = "climate")
public class Climate {
    @Id
    private String id;
    private Integer prespredictHour;
    private Integer pressure;
    private ClimateData climateData;

    @Builder
    public Climate(Integer prespredictHour, Integer pressure, ClimateData climateData) {
        this.prespredictHour = prespredictHour;
        this.pressure = pressure;
        this.climateData = climateData;
    }


    public static class ClimateData {
        private String[][] UVVector;
        private Integer pressure;
        private Integer predictHour;
        private Integer uvVectorIndex;

        @Builder
        public ClimateData(String[][] UVVector, Integer pressure, Integer predictHour, Integer uvVectorIndex) {
            this.UVVector = UVVector;
            this.pressure = pressure;
            this.predictHour = predictHour;
            this.uvVectorIndex = uvVectorIndex;
        }
    }
}

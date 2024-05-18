package example.cashcard;

import org.springframework.data.annotation.Id;

public record CashCard(
        @Id
        Long id,
        Double amount) {


        public CashCard {
        }

        @Override
        public Long id() {
                return id;
        }

        @Override
        public Double amount() {
                return amount;
        }
}

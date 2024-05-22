package example.cashcard;

import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import net.minidev.json.JSONArray;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.annotation.DirtiesContext;

import java.net.URI;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.annotation.DirtiesContext.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DirtiesContext(classMode = ClassMode.AFTER_EACH_TEST_METHOD)
class CashCardApplicationTests {

	@Autowired
	TestRestTemplate testRestTemplate;

	@Test
	void shouldReturnACashCardWhenDataIsSaved(){

		ResponseEntity<String> response = testRestTemplate.getForEntity("/cashcards/99",String.class);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

		DocumentContext documentContext = JsonPath.parse(response.getBody());
		Number id = documentContext.read("$.id");
		assertThat(id.longValue()).isEqualTo(99);

		Double amount = documentContext.read("$.amount");
		assertThat(amount).isEqualTo(123.45);
	}

	@Test
	void shouldNotReturnACashCardWithAnUnknownId() {
		ResponseEntity<String> response = testRestTemplate.getForEntity("/cashcards/1000", String.class);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
		assertThat(response.getBody()).isBlank();
	}

	@Test
	void shouldCreateNewCashCard(){

		CashCard cashCard = new CashCard(null, 250.90);
		ResponseEntity<Void> response = testRestTemplate.postForEntity("/cashcards", cashCard, Void.class);
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);

		URI locationOfNewCashCard = response.getHeaders().getLocation();
		ResponseEntity<String> getResponse = testRestTemplate.getForEntity(locationOfNewCashCard, String.class);
		assertThat(getResponse.getStatusCode()).isEqualTo(HttpStatus.OK);

		DocumentContext context = JsonPath.parse(getResponse.getBody());

		Number id = context.read("$.id");
		Double amount = context.read("$.amount");

		assertThat(id).isNotNull();
		assertThat(amount).isEqualTo(250.90);
	}


	@Test
	void shouldReturnAllCashCardsWhenListIsRequested() {
		ResponseEntity<String> response = testRestTemplate.getForEntity("/cashcards?page=0&size=3", String.class);
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

		DocumentContext documentContext = JsonPath.parse(response.getBody());

		int cashCardCount = documentContext.read("$.length()");
		assertThat(cashCardCount).isEqualTo(3);

		JSONArray ids = documentContext.read("$..id");
		assertThat(ids).containsExactlyInAnyOrder(99, 100, 101);

		JSONArray amounts = documentContext.read("$..amount");
		assertThat(amounts).containsExactlyInAnyOrder(123.45, 1.0, 150.00);

		JSONArray page = documentContext.read("$[*]");
		assertThat(page.size()).isEqualTo(3);
	}

	@Test
	void shouldReturnASortedPageOfCashCards(){
		ResponseEntity<String> response = testRestTemplate.getForEntity("/cashcards?page=0&size=1&sort=amount,asc", String.class);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

		DocumentContext documentContext = JsonPath.parse(response.getBody());

		JSONArray page = documentContext.read("$[*]");

		assertThat(page.size()).isEqualTo(1);

		double amount = documentContext.read("$[0].amount");
		assertThat(amount).isEqualTo(1.00);
	}

	@Test
	void shouldReturnASortedPageOfCashCardsWithoutParametersAndUseDefaultValues(){
		ResponseEntity<String> response = testRestTemplate.getForEntity("/cashcards", String.class);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

		DocumentContext documentContext = JsonPath.parse(response.getBody());

		JSONArray page = documentContext.read("$[*]");
		assertThat(page.size()).isEqualTo(3);

		JSONArray amount = documentContext.read("$..amount");
		assertThat(amount).containsExactly( 1.0, 123.45, 150.00);

	}

}

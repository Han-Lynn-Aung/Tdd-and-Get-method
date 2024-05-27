package example.cashcard;

import com.jayway.jsonpath.*;
import net.minidev.json.JSONArray;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;
import org.springframework.test.annotation.DirtiesContext;

import java.net.URI;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class CashCardApplicationTests {

    @Autowired
    TestRestTemplate testRestTemplate;

    @Test
    void shouldReturnACashCardWhenDataIsSaved() {

        ResponseEntity<String> response = testRestTemplate.withBasicAuth("sarah1", "password")
                .getForEntity("/cashcards/100", String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

        DocumentContext documentContext = JsonPath.parse(response.getBody());
        Number id = documentContext.read("$.id");
        assertThat(id.longValue()).isEqualTo(100);

        Double amount = documentContext.read("$.amount");
        assertThat(amount).isEqualTo(1.00);
    }

    @Test
    void shouldNotReturnACashCardWithAnUnknownId() {
        ResponseEntity<String> response = testRestTemplate.withBasicAuth("sarah1", "password")
                .getForEntity("/cashcards/1000", String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isBlank();
    }

    @Test
// @DirtiesContext
    void shouldCreateNewCashCard() {

        // Step 1: Create a new CashCard instance
        CashCard cashCard = new CashCard(100L, 1.00, "sarah1");

        // Step 2: Post the new CashCard with authentication
        ResponseEntity<Void> response = testRestTemplate.withBasicAuth("sarah1", "password")
                .postForEntity("/cashcards", cashCard, Void.class);

        // Step 3: Verify that the status code is CREATED
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);

        // Step 4: Get the location of the new CashCard
        URI locationOfNewCashCard = response.getHeaders().getLocation();

        // Step 5: Perform a GET request without authentication to ensure it's unauthorized
        ResponseEntity<String> getResponse = testRestTemplate.getForEntity(locationOfNewCashCard, String.class);

        // Step 6: Verify that the status code is UNAUTHORIZED
        assertThat(getResponse.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);

        // Step 7: Perform a GET request with authentication to retrieve the CashCard details
        ResponseEntity<String> authenticatedGetResponse = testRestTemplate.withBasicAuth("sarah1", "password")
                .getForEntity(locationOfNewCashCard, String.class);

        // Step 8: Verify that the status code is OK
        assertThat(authenticatedGetResponse.getStatusCode()).isEqualTo(HttpStatus.OK);

        // Step 9: Parse the JSON response
        DocumentContext context = JsonPath.parse(authenticatedGetResponse.getBody());

        // Step 10: Extract the id and amount fields from the JSON response
        Number id = context.read("$.id");
        Double amount = context.read("$.amount");

        // Step 11: Assert that the retrieved id and amount match the expected values
        assertThat(id).isEqualTo(100);
        assertThat(amount).isEqualTo(1.00);
    }


    @Test
    void shouldReturnAllCashCardsWhenListIsRequested() {
        ResponseEntity<String> response = testRestTemplate.withBasicAuth("sarah1", "password")
                .getForEntity("/cashcards?page=0&size=1", String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

        DocumentContext documentContext = JsonPath.parse(response.getBody());

        int cashCardCount = documentContext.read("$.length()");
        assertThat(cashCardCount).isEqualTo(1);

        JSONArray ids = documentContext.read("$..id");
        assertThat(ids).containsExactlyInAnyOrder(100);

        JSONArray amounts = documentContext.read("$..amount");
        assertThat(amounts).containsExactlyInAnyOrder(1.0);

        JSONArray page = documentContext.read("$[*]");
        assertThat(page.size()).isEqualTo(1);
    }

    @Test
    void shouldReturnASortedPageOfCashCards() {
        ResponseEntity<String> response = testRestTemplate.withBasicAuth("sarah1", "password")
                .getForEntity("/cashcards?page=0&size=1&sort=amount,asc", String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

        DocumentContext documentContext = JsonPath.parse(response.getBody());

        JSONArray page = documentContext.read("$[*]");

        assertThat(page.size()).isEqualTo(1);

        double amount = documentContext.read("$[0].amount");
        assertThat(amount).isEqualTo(1.00);
    }

    @Test
    void shouldReturnASortedPageOfCashCardsWithoutParametersAndUseDefaultValues() {
        ResponseEntity<String> response = testRestTemplate.
                withBasicAuth("sarah1", "password")
                .getForEntity("/cashcards", String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

        DocumentContext documentContext = JsonPath.parse(response.getBody());

        JSONArray page = documentContext.read("$[*]");
        assertThat(page.size()).isEqualTo(2);

        JSONArray amount = documentContext.read("$..amount");
        assertThat(amount).containsExactly(1.0, 123.45);

    }

    @Test
    void shouldReturnACashCardWhenIsSaved() {
        ResponseEntity<String> response = testRestTemplate
                .withBasicAuth("sarah1", "password")
                .getForEntity("/cashcards/100", String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void shouldNotReturnACashCardWhenUsingBadCredentials() {
        ResponseEntity<String> response = testRestTemplate
                .withBasicAuth("BAD-USER", "password")
                .getForEntity("/cashcards/99", String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);

        response = testRestTemplate
                .withBasicAuth("sarah1", "BAD-PASSWORD")
                .getForEntity("/cashcards/99", String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void shouldRejectUsersWhoAreNotCardOwners() {
        ResponseEntity<String> response = testRestTemplate
                .withBasicAuth("hank-owns-no-cards", "password")
                .getForEntity("/cashcards/99", String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void shouldNotAllowAccessToCashCardsTheyDoNotOwn() {
        ResponseEntity<String> response = testRestTemplate
                .withBasicAuth("sarah1", "password")
                .getForEntity("/cashcards/102", String.class); // kumar2's data
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    @DirtiesContext
    void shouldUpdateAnExistingCashCard() {
        CashCard cashCardUpdate = new CashCard(null, 19.99, null);
        HttpEntity<CashCard> request = new HttpEntity<>(cashCardUpdate);
        ResponseEntity<Void> response = testRestTemplate
                .withBasicAuth("sarah1", "password")
                .exchange("/cashcards/999", HttpMethod.PUT, request, Void.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);

        ResponseEntity<String> getResponse = testRestTemplate
                .withBasicAuth("sarah1", "password")
                .getForEntity("/cashcards/99", String.class);
        assertThat(getResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        DocumentContext documentContext = JsonPath.parse(getResponse.getBody());
        Number id = documentContext.read("$.id");
        Double amount = documentContext.read("$.amount");
        assertThat(id).isEqualTo(99);
        assertThat(amount).isEqualTo(123.45);
    }
}

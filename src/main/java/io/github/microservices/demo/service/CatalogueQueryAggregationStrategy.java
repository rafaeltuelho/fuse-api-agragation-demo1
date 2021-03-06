package io.github.microservices.demo.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.apache.camel.Exchange;
import org.apache.camel.processor.aggregate.AggregationStrategy;

public class CatalogueQueryAggregationStrategy implements AggregationStrategy {

  @Override
  public Exchange aggregate(Exchange ex1, Exchange ex2) {
    if (ex1 == null) {
      return ex2;
    } else {
      Product product = null;

      int sockResponseCode1 = Integer.valueOf(ex1.getIn().getHeader(Exchange.HTTP_RESPONSE_CODE, String.class));
      int shoeResponseCode = Integer.valueOf(ex2.getIn().getHeader(Exchange.HTTP_RESPONSE_CODE, String.class));

      // when both backend catalogue services return nothing...
      if (sockResponseCode1 >= 400 && shoeResponseCode >= 400) {

        CatalogueErrorResponse errorResponse = new CatalogueErrorResponse();
        errorResponse.setErrorMsg("not able to query catalogue backends: " + 
            "Socks catalogue backend returned HTTP " + sockResponseCode1 + 
            "; Shoes catalogue backend returned HTTP " + shoeResponseCode);
        errorResponse.setStatusCode(500);
        errorResponse.setStatusMsg("Internal Server Error");

        ObjectMapper mapper = new ObjectMapper();
        String jsonString = "{ }";
        try {
          jsonString = mapper.writeValueAsString(errorResponse);
        } catch (JsonProcessingException e) {
          e.printStackTrace();
        }

        ex1.getIn().setHeader(Exchange.CONTENT_TYPE, "application/json");
        ex1.getIn().setHeader(Exchange.HTTP_RESPONSE_CODE, 404);
        ex1.getIn().setBody(jsonString);
        
        return ex1;
      }
      
      if (sockResponseCode1 == 200){
        Sock sock = ex1.getIn().getBody(Sock.class);
        if (sock != null){
          System.out.println("sock found with id: " + sock.getId());
          product = sock;
        }
      }
      
      if (shoeResponseCode == 200){
        Shoe shoe = ex2.getIn().getBody(Shoe.class);
        if (shoe != null){
          System.out.println("shoe id: " + shoe.getId());
          product = new Product();
          product.setId(shoe.getId());
          product.setName(shoe.getName());
          product.setDescription(shoe.getDescription());
          product.setCount(shoe.getCount());
          product.setPrice(shoe.getPrice());
          product.setProductType(shoe.getProductType());
          product.addImageUrl(shoe.getImageUrl1());
          product.addImageUrl(shoe.getImageUrl2());
          for (Tag tag : shoe.getTags()) {
            product.addTag(tag.getName());
          }
        }
      }

      ex1.getIn().setBody(product);
      ex1.getIn().setHeader(Exchange.CONTENT_TYPE, "application/json");
      ex1.getIn().setHeader(Exchange.HTTP_RESPONSE_CODE, 200);
      return ex1;
    }
  }
}

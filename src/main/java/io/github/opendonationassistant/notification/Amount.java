package io.github.opendonationassistant.notification;

import io.micronaut.serde.annotation.Serdeable;

@Serdeable
public class Amount {

  private Integer minor;
  private Integer major;
  private String currency;

  public Amount(Integer minor, Integer major, String currency) {
    this.minor = minor;
    this.major = major;
    this.currency = currency;
  }

  public Integer getMinor() {
    return minor;
  }

  public Integer getMajor() {
    return major;
  }

  public String getCurrency() {
    return currency;
  }

  @Override
  public String toString() {
    return (
      "Amount [minor=" +
      minor +
      ", major=" +
      major +
      ", currency=" +
      currency +
      "]"
    );
  }
}

package com.restaurant_management.restaurant_management_backend.entity;

import com.restaurant_management.restaurant_management_backend.enums.TableStatus;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TableEntityTest {

  @Test
  void occupy_changesStatusToOccupied() {
    Table table = Table.builder().status(TableStatus.FREE).build();

    table.occupy();

    assertThat(table.getStatus()).isEqualTo(TableStatus.OCCUPIED);
  }

  @Test
  void occupy_throwsWhenAlreadyOccupied() {
    Table table = Table.builder().status(TableStatus.OCCUPIED).build();

    assertThatThrownBy(table::occupy)
      .isInstanceOf(IllegalStateException.class)
      .hasMessageContaining("ocupada");
  }

  @Test
  void free_changesStatusToFree() {
    Table table = Table.builder().status(TableStatus.OCCUPIED).build();

    table.free();

    assertThat(table.getStatus()).isEqualTo(TableStatus.FREE);
  }

  @Test
  void free_throwsWhenNotOccupied() {
    Table table = Table.builder().status(TableStatus.FREE).build();

    assertThatThrownBy(table::free)
      .isInstanceOf(IllegalStateException.class);
  }

  @Test
  void reserve_changesStatusToReserved() {
    Table table = Table.builder().status(TableStatus.FREE).build();

    table.reserve();

    assertThat(table.getStatus()).isEqualTo(TableStatus.RESERVED);
  }

  @Test
  void reserve_throwsWhenNotFree() {
    Table table = Table.builder().status(TableStatus.OCCUPIED).build();

    assertThatThrownBy(table::reserve)
      .isInstanceOf(IllegalStateException.class);
  }
}

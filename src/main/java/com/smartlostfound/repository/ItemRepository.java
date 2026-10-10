
package com.smartlostfound.repository;

import com.smartlostfound.entity.Item;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ItemRepository extends JpaRepository<Item, Long> {

    List<Item> findByStatusIgnoreCase(String status);

    List<Item> findByTitleContainingIgnoreCase(String title);
}

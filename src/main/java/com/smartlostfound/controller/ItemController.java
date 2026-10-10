
package com.smartlostfound.controller;

import com.smartlostfound.entity.Item;
import com.smartlostfound.repository.ItemRepository;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@RestController
@RequestMapping("/api/items")
@CrossOrigin(origins = "http://localhost:5173")
public class ItemController {

    private final ItemRepository itemRepository;

    private final Path uploadDirectory =
            Paths.get("uploads").toAbsolutePath().normalize();

    private static final long MAX_IMAGE_SIZE = 5 * 1024 * 1024;

    public ItemController(ItemRepository itemRepository) {
        this.itemRepository = itemRepository;
    }

    // Add an item using JSON
    @PostMapping
    public Item addItem(@RequestBody Item item) {
        validateItem(item);
        item.setStatus(item.getStatus().toUpperCase(Locale.ROOT));
        return itemRepository.save(item);
    }

    // Add an item with an image
    @PostMapping(value = "/with-image", consumes = "multipart/form-data")
    public Item addItemWithImage(
            @RequestParam("title") String title,
            @RequestParam("description") String description,
            @RequestParam("category") String category,
            @RequestParam("location") String location,
            @RequestParam("status") String status,
            @RequestParam("contactInfo") String contactInfo,
            @RequestParam("image") MultipartFile image) {

        if (title == null || title.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Title is required");
        }

        if (status == null ||
                !(status.equalsIgnoreCase("LOST")
                        || status.equalsIgnoreCase("FOUND"))) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Status must be LOST or FOUND");
        }

        if (image == null || image.isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Please select an image");
        }

        if (image.getSize() > MAX_IMAGE_SIZE) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Image must be 5 MB or smaller");
        }

        String contentType = image.getContentType();

        if (contentType == null ||
                !Set.of("image/jpeg", "image/png", "image/webp")
                        .contains(contentType.toLowerCase(Locale.ROOT))) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Only JPG, PNG, and WebP images are allowed");
        }

        String extension;

        switch (contentType.toLowerCase(Locale.ROOT)) {
            case "image/jpeg":
                extension = ".jpg";
                break;
            case "image/png":
                extension = ".png";
                break;
            case "image/webp":
                extension = ".webp";
                break;
            default:
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST, "Unsupported image format");
        }

        String filename = UUID.randomUUID() + extension;
        Path destination = uploadDirectory.resolve(filename).normalize();

        if (!destination.startsWith(uploadDirectory)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Invalid image path");
        }

        try {
            Files.createDirectories(uploadDirectory);
            image.transferTo(destination);

            Item item = new Item();
            item.setTitle(title.trim());
            item.setDescription(description);
            item.setCategory(category);
            item.setLocation(location);
            item.setStatus(status.toUpperCase(Locale.ROOT));
            item.setContactInfo(contactInfo);
            item.setImageUrl("/uploads/" + filename);

            return itemRepository.save(item);

        } catch (IOException exception) {
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Could not save the uploaded image");
        }
    }

    // Get all items
    @GetMapping
    public List<Item> getAllItems() {
        return itemRepository.findAll();
    }

    // Get one item
    @GetMapping("/{id}")
    public Item getItemById(@PathVariable Long id) {
        return itemRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Item not found"));
    }

    // Filter LOST or FOUND items
    @GetMapping("/status/{status}")
    public List<Item> getItemsByStatus(@PathVariable String status) {
        if (!(status.equalsIgnoreCase("LOST")
                || status.equalsIgnoreCase("FOUND"))) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Status must be LOST or FOUND");
        }

        return itemRepository.findByStatusIgnoreCase(status);
    }

    // Search items by title
    @GetMapping("/search")
    public List<Item> searchItems(@RequestParam String title) {
        return itemRepository.findByTitleContainingIgnoreCase(title);
    }

    // Mark an item as returned
    @PutMapping("/{id}/return")
    public Item markAsReturned(@PathVariable Long id) {
        Item item = itemRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Item not found"));

        if (!"LOST".equalsIgnoreCase(item.getStatus())
                && !"FOUND".equalsIgnoreCase(item.getStatus())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Only LOST or FOUND items can be marked as returned");
        }

        item.setStatus("RETURNED");
        return itemRepository.save(item);
    }

    // Delete an item permanently
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteItem(@PathVariable Long id) {
        if (!itemRepository.existsById(id)) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND, "Item not found");
        }

        itemRepository.deleteById(id);
    }

    // Validate new reports
    private void validateItem(Item item) {
        if (item.getTitle() == null || item.getTitle().isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Title is required");
        }

        if (item.getStatus() == null ||
                !(item.getStatus().equalsIgnoreCase("LOST")
                        || item.getStatus().equalsIgnoreCase("FOUND"))) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Status must be LOST or FOUND");
        }
    }
}

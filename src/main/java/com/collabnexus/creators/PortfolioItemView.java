package com.collabnexus.creators;

public record PortfolioItemView(
        Long id,
        String title,
        String url,
        String description
) {

    public static PortfolioItemView from(CreatorPortfolioItem item) {
        return new PortfolioItemView(item.getId(), item.getTitle(), item.getUrl(), item.getDescription());
    }
}

package com.lirasemijoias.projeto.model;

public class ProductImage {
    private String url;
    private String publicId;
    private boolean main;

    public ProductImage() {}

    public ProductImage(String url, String publicId, boolean main) {
        this.url = url;
        this.publicId = publicId;
        this.main = main;
    }

    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }
    public String getPublicId() { return publicId; }
    public void setPublicId(String publicId) { this.publicId = publicId; }
    public boolean isMain() { return main; }
    public void setMain(boolean main) { this.main = main; }
}

package it.smartcommunitylab.aac.model;

public class Realm {

    private String name;
    private String slug;

    public Realm() {

    }

    public Realm(String slug, String name) {
        this.name = name;
        this.slug = slug;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSlug() {
        return slug;
    }

    public void setSlug(String slug) {
        this.slug = slug;
    }

}

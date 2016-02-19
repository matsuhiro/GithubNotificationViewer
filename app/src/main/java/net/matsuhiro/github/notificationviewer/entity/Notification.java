package net.matsuhiro.github.notificationviewer.entity;

import com.google.gson.annotations.SerializedName;

public class Notification {

    @SerializedName("updated_at")
    public String lastUpdateAt;

    @SerializedName("subject")
    public Subject subject;

    @SerializedName("repository")
    public Repository repository;

    public class Subject {
        @SerializedName("title")
        public String title;
        @SerializedName("url")
        public String url;
    }

    public class Repository {
        @SerializedName("name")
        public String name;
    }
}

package it.smartcommunitylab.aac.core;

import org.springframework.security.core.AuthenticatedPrincipal;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

public class Subject implements AuthenticatedPrincipal {

    private final String subject;
    private final String name;

    public Subject(String subject, String name) {
        Assert.notNull(subject, "subject can not be null");
        this.subject = subject;
        if (StringUtils.hasText(name)) {
            this.name = name;
        } else {
            this.name = subject;
        }
    }

    @Override
    public String getName() {
        return name;
    }

    public String getSubject() {
        return subject;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((subject == null) ? 0 : subject.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        Subject other = (Subject) obj;
        if (subject == null) {
            if (other.subject != null)
                return false;
        } else if (!subject.equals(other.subject))
            return false;
        return true;
    }

    @Override
    public String toString() {
        return "Subject [subject=" + subject + ", name=" + name + "]";
    }

}

package it.smartcommunitylab.aac.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import it.smartcommunitylab.aac.Config;
import javax.validation.Valid;
import javax.validation.constraints.Size;
import org.springframework.util.StringUtils;

@Valid
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(Include.NON_NULL)
public class SpaceRole {

    // role definition
    private String context;

    @Size(max = 128)
    private String space;

    @Size(max = 128)
    private String role;

    public SpaceRole(String context, String space, String role) {
        super();
        this.context = StringUtils.hasText(context) ? context : null;
        this.space = StringUtils.hasText(space) ? space : null;
        this.role = role;
        validate(this);
    }

    protected SpaceRole() {
        this.context = null;
        this.space = null;
        this.role = null;
    }

    public String getContext() {
        return context;
    }

    public void setContext(String context) {
        this.context = context;
    }

    public String getSpace() {
        return space;
    }

    public void setSpace(String space) {
        this.space = space;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    @Override
    public String toString() {
        return "Role [context=" + context + ", space=" + space + ", role=" + role + "]";
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((context == null) ? 0 : context.hashCode());
        result = prime * result + ((role == null) ? 0 : role.hashCode());
        result = prime * result + ((space == null) ? 0 : space.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null) return false;
        if (getClass() != obj.getClass()) return false;
        SpaceRole other = (SpaceRole) obj;
        if (context == null) {
            if (other.context != null) return false;
        } else if (!context.equals(other.context)) return false;
        if (role == null) {
            if (other.role != null) return false;
        } else if (!role.equals(other.role)) return false;
        if (space == null) {
            if (other.space != null) return false;
        } else if (!space.equals(other.space)) return false;
        return true;
    }

    public String canonicalSpace() {
        StringBuilder sb = new StringBuilder();
        if (StringUtils.hasText(context)) {
            sb.append(context);
            sb.append('/');
        }
        if (StringUtils.hasText(space)) {
            sb.append(space);
        }
        return sb.toString();
    }

    public String asSlug() {
        return canonicalSpace().replace('/', '-');
    }

    public String getAuthority() {
        StringBuilder sb = new StringBuilder();
        if (StringUtils.hasText(context)) {
            sb.append(context);
            sb.append('/');
        }
        if (StringUtils.hasText(space)) {
            sb.append(space);
            sb.append(':');
        }
        sb.append(role);
        return sb.toString();
    }

    public static SpaceRole systemUser() {
        return new SpaceRole(null, null, Config.R_USER);
    }

    public static SpaceRole systemAdmin() {
        return new SpaceRole(null, null, Config.R_ADMIN);
    }

    public static SpaceRole systemDeveloper() {
        return new SpaceRole(null, null, Config.R_DEVELOPER);
    }

    public static SpaceRole ownerOf(String ctxStr) {
        int idx = ctxStr.lastIndexOf('/');
        String ctx = idx > 0 ? ctxStr.substring(0, idx) : null;
        String space = idx > 0 ? ctxStr.substring(idx + 1) : ctxStr;
        return new SpaceRole(ctx, space, Config.R_PROVIDER);
    }

    public static SpaceRole memberOf(String ctxStr, String role) {
        int idx = ctxStr.lastIndexOf('/');
        String ctx = idx > 0 ? ctxStr.substring(0, idx) : null;
        String space = idx > 0 ? ctxStr.substring(idx + 1) : ctxStr;
        SpaceRole r = new SpaceRole(ctx, space, role);
        validate(r);
        return r;
    }

    public static SpaceRole parse(String s) throws IllegalArgumentException {
        s = s.trim();
        int idx = s.lastIndexOf(':');
        if (!StringUtils.hasText(s) || idx == s.length() - 1) throw new IllegalArgumentException(
            "Invalid Role format " + s
        );
        if (idx <= 0) return new SpaceRole(null, null, s.substring(idx + 1));
        return memberOf(s.substring(0, idx), s.substring(idx + 1));
    }

    public static void validate(SpaceRole r) throws IllegalArgumentException {
        // context may be empty
        if (r.context != null && !r.context.matches("[\\w\\./]+")) {
            throw new IllegalArgumentException(
                "Invalid role context value: only alpha-numeric characters and '_./' allowed"
            );
        }

        // space empty only if context is empty
        if (r.space == null && r.context != null || r.space != null && !r.space.matches("[\\w\\.]+")) {
            throw new IllegalArgumentException(
                "Invalid role space value: only alpha-numeric characters and '_.' allowed"
            );
        }

        // role should never be empty
        if (r.role == null || !r.role.matches("[\\w\\.]+")) {
            throw new IllegalArgumentException("Invalid role value: only alpha-numeric characters and '_.' allowed");
        }
    }
}

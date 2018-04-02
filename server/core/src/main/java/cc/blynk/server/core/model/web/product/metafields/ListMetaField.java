package cc.blynk.server.core.model.web.product.metafields;

import cc.blynk.server.core.model.web.Role;
import cc.blynk.server.core.model.web.product.MetaField;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 04.04.17.
 */
public class ListMetaField extends MetaField {

    public final String[] options;

    @JsonCreator
    public ListMetaField(@JsonProperty("id") int id,
                         @JsonProperty("name") String name,
                         @JsonProperty("role") Role role,
                         @JsonProperty("isDefault") boolean isDefault,
                         @JsonProperty("options") String[] options) {
        super(id, name, role, isDefault);
        this.options = options;
    }

    @Override
    public MetaField copy() {
        return new ListMetaField(id, name, role, isDefault, options);
    }
}

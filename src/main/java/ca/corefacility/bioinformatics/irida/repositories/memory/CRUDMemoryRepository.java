/*
 * Copyright 2013 Franklin Bristow <franklin.bristow@phac-aspc.gc.ca>.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ca.corefacility.bioinformatics.irida.repositories.memory;

import ca.corefacility.bioinformatics.irida.model.enums.Order;
import ca.corefacility.bioinformatics.irida.model.roles.Identifiable;
import ca.corefacility.bioinformatics.irida.model.roles.impl.Identifier;
import ca.corefacility.bioinformatics.irida.repositories.CRUDRepository;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A base class for CRUD memory repositories. DO NOT USE THIS CLASS IN
 * PRODUCTION.
 *
 * @author Franklin Bristow <franklin.bristow@phac-aspc.gc.ca>
 */
public class CRUDMemoryRepository<Type extends Identifiable<Identifier>> implements CRUDRepository<Identifier, Type> {

    protected Map<Identifier, Type> store = new HashMap<>();
    protected Class<Type> valueType;

    public CRUDMemoryRepository(Class<Type> valueType) {
        this.valueType = valueType;
    }

    @Override
    public Type create(Type object) throws IllegalArgumentException {
        Identifier id = new Identifier();
        object.setIdentifier(id);
        store.put(id, object);
        return object;
    }

    @Override
    public Type read(Identifier id) throws IllegalArgumentException {
        if (exists(id)) {
            return store.get(id);
        }

        throw new IllegalArgumentException("No such object with the given identifier exists.");
    }

    @Override
    public Type update(Type object) throws IllegalArgumentException {
        Identifier id = object.getIdentifier();

        if (exists(id)) {
            return store.put(id, object);
        }

        throw new IllegalArgumentException("No such object with the given identifier exists.");
    }

    @Override
    public void delete(Identifier id) {
        store.remove(id);
    }

    @Override
    public List<Type> list() {
        return new ArrayList<>(store.values());
    }

    @Override
    public Boolean exists(Identifier id) {
        return store.containsKey(id);
    }

    @Override
    public List<Type> list(int page, int size, final String sortProperty, final Order order) {
        List<Type> values = list();
        Collections.sort(values, new Comparator<Type>() {
            @Override
            public int compare(Type o1, Type o2) {
                if (order.equals(Order.ASCENDING)) {
                    return getValue(o1, sortProperty).compareTo(getValue(o2, sortProperty));
                } else {
                    return getValue(o2, sortProperty).compareTo(getValue(o1, sortProperty));
                }
            }
        });
        int start = size * (page - 1);
        return values.subList(start, start + size);
    }

    /**
     * Get the value of property from the instance of
     * <code>ValueType</code>.
     *
     * @param object the instance to get the property value from.
     * @param property the property name to get the value of.
     * @return the value of the property on the instance.
     * @throws IllegalArgumentException if no public method exists on the object
     * corresponding to the property.
     */
    private <PropertyType extends Comparable> PropertyType getValue(Type object, String property) throws IllegalArgumentException {
        String methodName = getMethodName(property);
        try {
            Method m = valueType.getDeclaredMethod(methodName);
            return (PropertyType) m.invoke(object);
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            throw new IllegalArgumentException("No method exists on type [" + valueType + "] for property [" + property + "] (is the method public?)");
        } catch (ClassCastException e) {
            throw new IllegalArgumentException("Property [" + property + "] is not comparable.");
        }
    }

    /**
     * Tests whether or not the supplied property actually applies to the type
     * of object that we're persisting with this class.
     *
     * @param property the name of the property to test
     * @return true if a getter exists for the property, false otherwise
     */
    private boolean methodAvailable(String property) {
        String methodName = getMethodName(property);
        try {
            Method m = valueType.getDeclaredMethod(methodName);
        } catch (NoSuchMethodException e) {
            return false;
        }
        return true;
    }

    /**
     * Get the standard getter name for the specified property.
     *
     * @param property the name of the property.
     * @return the standard getter name for the property.
     */
    private String getMethodName(String property) {
        StringBuilder builder = new StringBuilder("get");
        builder.append(property.substring(0, 1).toUpperCase());
        builder.append(property.substring(1));
        return builder.toString();
    }
}
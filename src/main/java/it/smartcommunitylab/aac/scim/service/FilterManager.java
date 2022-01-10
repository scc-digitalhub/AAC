/*******************************************************************************

 * Copyright 2015 Fondazione Bruno Kessler
 * 
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 * 
 *        http://www.apache.org/licenses/LICENSE-2.0
 * 
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 ******************************************************************************/

package it.smartcommunitylab.aac.scim.service;

import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.Date;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Expression;
import javax.persistence.criteria.Path;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

import org.springframework.data.jpa.domain.Specification;
import org.wso2.charon3.core.exceptions.BadRequestException;
import org.wso2.charon3.core.schema.SCIMConstants;
import org.wso2.charon3.core.utils.codeutils.ExpressionNode;
import org.wso2.charon3.core.utils.codeutils.Node;
import org.wso2.charon3.core.utils.codeutils.OperationNode;


/**
 * @author raman
 *
 */
public class FilterManager {

	@SuppressWarnings("serial")
	public static <T> Specification<T> buildQuery(Node root, String realm) {
		Specification<T> spec = new Specification<T>() {

			@Override
			public Predicate toPredicate(Root<T> entityRoot, CriteriaQuery<?> query, CriteriaBuilder criteriaBuilder) {
				return criteriaBuilder.and(
						createPredicate(root, entityRoot, criteriaBuilder),
						criteriaBuilder.equal(entityRoot.get("realm"), criteriaBuilder.literal(realm)));
			}
			
		};
		
		return spec;
	}

	/**
	 * @param root
	 * @param cb 
	 * @return
	 * @throws BadRequestException 
	 */
	protected static <T>  Predicate createPredicate(Node root, Root<T> er, CriteriaBuilder cb) {
		if (root instanceof OperationNode) {
			switch(((OperationNode) root).getOperation()) {
			case SCIMConstants.OperationalConstants.AND:
				return cb.and(createPredicate(root.getLeftNode(), er, cb), createPredicate(root.getRightNode(), er, cb));
			case SCIMConstants.OperationalConstants.OR:
				return cb.or(createPredicate(root.getLeftNode(), er, cb), createPredicate(root.getRightNode(), er, cb));
			case SCIMConstants.OperationalConstants.NOT:
				return cb.not(createRestriction(root.getRightNode(), er, cb));
			}
		}
		if (root instanceof ExpressionNode) {
			String attribute = ((ExpressionNode) root).getAttributeValue();
			attribute = normalizeAttribute(attribute);
			String value = ((ExpressionNode) root).getValue();
			String operation = ((ExpressionNode) root).getOperation().trim(); 
			switch (operation) {
			case "eq": {
				Path<Object> prop = er.get(attribute);
				if (attribute.equals("locked")) return cb.notEqual(prop, createValue(prop, value, cb));
				return cb.equal(prop, cb.literal(value));
			}
			case "ne": {
				Path<Object> prop = er.get(attribute);
				if (attribute.equals("locked")) return cb.equal(prop, cb.literal(value));
				return cb.notEqual(prop, cb.literal(value));
			}
			case "co": 
				return cb.like(er.get(attribute), cb.literal('%'+value+'%'));
			case "sw":
				return cb.like(er.get(attribute), cb.literal(value+'%'));
			case "ew":
				return cb.like(er.get(attribute), cb.literal('%'+value));
			case "pr":
				return cb.isNotNull(er.get(attribute));
			case "gt": {
				Path<String> prop = er.get(attribute);
				return cb.greaterThan(prop, createValue(prop, value, cb));
			}
			case "ge": {
				Path<String> prop = er.get(attribute);
				return cb.greaterThanOrEqualTo(prop, createValue(prop, value, cb));
			}
			case "lt":{
				Path<String> prop = er.get(attribute);
				return cb.lessThan(prop, createValue(prop, value, cb));
				}
			case "le":{
				Path<String> prop = er.get(attribute);
				return cb.lessThanOrEqualTo(prop, createValue(prop, value, cb));
			}
			}
		}
		throw new IllegalArgumentException("Invalid expression");
	}

	/**
	 * @param attribute
	 * @return
	 */
	private static String normalizeAttribute(String attribute) {
		switch (attribute) {
		case SCIMConstants.CommonSchemaConstants.ID_URI: return "uuid";
		case SCIMConstants.CommonSchemaConstants.CREATED_URI: return "createDate";
		case SCIMConstants.CommonSchemaConstants.LAST_MODIFIED_URI: return "modifiedDate";
		case SCIMConstants.CommonSchemaConstants.EXTERNAL_ID_URI: return "externalId";
		case SCIMConstants.UserSchemaConstants.USER_NAME_URI: return "username";
		case SCIMConstants.UserSchemaConstants.EMAILS_VALUE_URI: return "emailAddress";
		case SCIMConstants.UserSchemaConstants.ACTIVE_URI: return "locked";
		case SCIMConstants.GroupSchemaConstants.DISPLAY_NAME_URI: return "displayName";
		}
		throw new IllegalArgumentException("Invalid attribute: " + attribute);
	}

	/**
	 * @param rightNode
	 * @param cb
	 * @return
	 */
	private static <T> Expression<Boolean> createRestriction(Node node, Root<T> er, CriteriaBuilder cb) {
		return createPredicate(node, er, cb);
	}
	
	
	@SuppressWarnings("unchecked")
	private static <Y> Expression<Y> createValue(Path<Y> path, String value, CriteriaBuilder cb) {
		Class<? extends Y> cls = path.getJavaType();
		if (value.equals("null")) {
			return (Expression<Y>) cb.nullLiteral(cls); 
		}
		if (cls.isAssignableFrom(Boolean.class)) return (Expression<Y>)cb.literal(Boolean.parseBoolean(value));
		if (cls.isAssignableFrom(Number.class)) return (Expression<Y>)cb.literal(Double.parseDouble(value));
		if (cls.isAssignableFrom(Date.class)) return (Expression<Y>)cb.literal(Date.from(Instant.from(DateTimeFormatter.ISO_INSTANT.parse(value))));
		return (Expression<Y>) cb.literal(value);
	}
	
}

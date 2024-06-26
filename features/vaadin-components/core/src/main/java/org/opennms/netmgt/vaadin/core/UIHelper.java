/*
 * Licensed to The OpenNMS Group, Inc (TOG) under one or more
 * contributor license agreements.  See the LICENSE.md file
 * distributed with this work for additional information
 * regarding copyright ownership.
 *
 * TOG licenses this file to You under the GNU Affero General
 * Public License Version 3 (the "License") or (at your option)
 * any later version.  You may not use this file except in
 * compliance with the License.  You may obtain a copy of the
 * License at:
 *
 *      https://www.gnu.org/licenses/agpl-3.0.txt
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied.  See the License for the specific
 * language governing permissions and limitations under the
 * License.
 */
package org.opennms.netmgt.vaadin.core;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Throwables;
import com.google.common.io.ByteStreams;
import com.vaadin.v7.data.Validator;
import com.vaadin.server.Page;
import com.vaadin.server.Resource;
import com.vaadin.server.UserError;
import com.vaadin.ui.AbstractComponent;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickListener;
import com.vaadin.v7.ui.Field;
import com.vaadin.v7.ui.Label;
import com.vaadin.ui.Notification;
import com.vaadin.ui.Notification.Type;
import com.vaadin.ui.UI;

/**
 * This class provides several helper methods for ui stuff, e.g. creating a
 * button. So the amount of code is reduced generally.
 * 
 * @author Markus von Rüden
 */
public abstract class UIHelper {

	private static final Logger LOG = LoggerFactory.getLogger(UIHelper.class);

	// in ms
	public static int  DEFAULT_NOTIFICATION_DELAY = 3000;

	public static Button createButton(
			final String buttonCaption,
			final String buttonDescription,
			final Resource icon,
			final ClickListener clickListener) {
		Button button = new Button();
		button.setCaption(buttonCaption);
		button.setIcon(icon);
		if (buttonDescription != null) button.setDescription(buttonDescription);
		if (clickListener != null) button.addClickListener(clickListener);
		return button;
	}

	public static Label createLabel(String caption, String content) {
		Label label = new Label(content);
		label.setCaption(caption);
		return label;
	}

	/**
	 * Loads the <code>resourceName</code> from the classpath using the given
	 * <code>clazz</code>. If the resource couldn't be loaded an empty string is
	 * returned.
	 * 
	 * @param clazz
	 *            The class to use for loading the resource.
	 * @param resourceName
	 *            The name of the resource to be loaded (e.g.
	 *            /folder/filename.txt)
	 * @return The content of the file, each line separated by line.separator or
	 *         empty string if the resource does not exist.
	 */
	public static String loadContentFromFile(final Class<?> clazz, final String resourceName) {
		Objects.requireNonNull(clazz);
		Objects.requireNonNull(resourceName);
		// prevent NullPointerException

		// check if resource is there
		try (InputStream is = clazz.getResourceAsStream(resourceName);
			 ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {

			LOG.debug("Resource '{}' loaded from class '{}': '{}'", resourceName, clazz.getName(), is);
			if (is == null) {
				throw new NullPointerException(String.format("Resource '%s' not available", resourceName));
			}

			// resource is there, so we can try loading it
			ByteStreams.copy(is, outputStream);
			return outputStream.toString();
		} catch (IOException e) {
			LOG.error("Error while reading resource from '{}'.", resourceName, e);
			throw Throwables.propagate(e);
		}
	}

	/**
	 * Shows a validation error to the user.
	 * 
	 * @param errorMessage
	 *            the error message.
	 */
	public static void showValidationError(String errorMessage) {
		showNotification("Validation Error", errorMessage != null ? errorMessage : "An unknown error occurred.", Type.ERROR_MESSAGE);
	}

	public static <T extends UI> T getCurrent(Class<T> clazz) {
		final T ui = (T) UI.getCurrent();
		if (ui == null || !ui.isAttached()) {
			throw new IllegalStateException("UI is either null or not attached. Ensure it is invoked from within a VaadinRequest");
		}
		return ui;
	}

	public static void showNotification(String message) {
		showNotification(message, null, Type.ERROR_MESSAGE);
	}

	public static void showNotification(String title, String message, Type type) {
		showNotification(title, message, type, DEFAULT_NOTIFICATION_DELAY);
	}

	public static void showNotification(String title, String message, Type type, int delayMsec) {
		Notification notification = new Notification(title, message, type, true);
		notification.setDelayMsec(delayMsec);
		notification.show(Page.getCurrent());
	}

	/**
	 * Validates the given field and sets the component error accordingly.
	 * Please note, that the provided field must implement {@link Field} and must be a sub class of {@link AbstractComponent}.
	 *
	 * @param field The field to validate (must be a sub class of {@link AbstractComponent}).
	 * @param swallowValidationExceptions Indicates if an InvalidValueException is swallowed and not propagated.
	 *                             If false the first occurring InvalidValueException is thrown.
	 * @throws Validator.InvalidValueException If the field is not valid (see {@link Validator#validate(Object)}.
	 */
	public static void validateField(Field<?> field, boolean swallowValidationExceptions) throws Validator.InvalidValueException {
		if (field instanceof AbstractComponent && field.isEnabled()) {
			try {
				field.validate();
				((AbstractComponent) field).setComponentError(null);
			} catch (Validator.InvalidValueException ex) {
				// Some fields unify exceptions, we have to consider this
				if (ex.getMessage() == null) {
					ex = ex.getCauses()[0];
				}

				// set error message
				((AbstractComponent) field).setComponentError(new UserError(ex.getMessage()));
				if (!swallowValidationExceptions) {
					throw ex;
				}
			}
		}
	}

	public static void validateFields(Collection<Field<?>> fields, boolean swallowValidationExceptions) {
		for(Field<?> eachField : fields) {
			validateField(eachField, swallowValidationExceptions);
		}
	}
}

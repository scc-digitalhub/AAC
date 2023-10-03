import {
    useInput,
    InputProps,
    Labeled,
    InputHelperText,
    TextField,
    TextInput,
    RecordContext,
} from 'react-admin';
import { Fragment } from 'react';
import { RichTextInput } from 'ra-input-rich-text';
import Form from '@rjsf/core';
import validator from '@rjsf/validator-ajv8';

export const JSONSchemaFormInput = (props: JSONSchemaFormatInputProps) => {
    const {
        schema,
        uiSchema,
        label,
        helperText,
        onBlur,
        onChange,
        resource,
        source,
    } = props;

    const {
        id,
        field,
        fieldState: { isTouched, error },
        formState: { isSubmitted },
        isRequired,
    } = useInput({
        // Pass the event handlers to the hook but not the component as the field property already has them.
        // useInput will call the provided onChange and onBlur in addition to the default needed by react-hook-form.
        onChange,
        onBlur,
        ...props,
    });

    const labelProps = {
        isRequired,
        label,
        resource,
        source,
    };

    let data = {};
    if (field.value) {
        data = {
            selectableAuthGrantTypes: field.value['authorizedGrantTypes'],
            selectWidgetAuthMethods: field.value['authenticationMethods'],
            redirectUri: field.value['redirectUris'],
            selectWidgetAppType: field.value['applicationType'],
            firstParty: field.value['firstParty'],
            idToken: field.value['idTokenClaims'],
            refreshToken: field.value['refreshTokenRotation'],
            selectWidgetSubjectType: field.value['subjectType'],
            selectWidgetTokenType: field.value['tokenType'],
            accessTokenValidity: field.value['accessTokenValidity'],
            refreshTokenValidity: field.value['refreshTokenValidity'],
        };
    }

    const log = (type: any) => {
        return console.log.bind(console, type);
    };

    return (
        <Fragment>
            <Labeled {...labelProps}>
                {/* <input id={id} {...field} /> */}
                <Form
                    schema={schema}
                    uiSchema={uiSchema}
                    formData={data}
                    validator={validator}
                    onChange={log('changed')}
                    onSubmit={log('submitted')}
                    onError={log('errors')}
                >
                    <div>
                        <button hidden type="submit"></button>
                    </div>
                </Form>
            </Labeled>
            <InputHelperText
                touched={isTouched || isSubmitted}
                error={error?.message}
                helperText={helperText}
            />
        </Fragment>
    );
};

export type JSONSchemaFormatInputProps = InputProps & {
    schema?: any;
    uiSchema?: any;
};

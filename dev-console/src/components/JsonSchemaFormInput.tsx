import { useInput, InputProps, Labeled, InputHelperText } from 'react-admin';
import { Fragment, useEffect } from 'react';
import { useFormContext } from 'react-hook-form';
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

    const update = (data: any) => {
        field.onChange(data);
    };

    return (
        <Fragment>
            <Labeled {...labelProps}>
                <Form
                    schema={schema}
                    uiSchema={uiSchema}
                    formData={field.value}
                    validator={validator}
                    onChange={e => update(e.formData)}
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

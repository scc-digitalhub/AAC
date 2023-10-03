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

    const formContext = useFormContext();
    let updatedData = {};

    // moved the setValue into a useEffect
    useEffect(() => {
        // if (updatedData) {
        // formContext.setValue('configuration', updatedData);
        // }
    });

    // if (field.value) {
    //     updatedData = field.value;
    // }

    const update = (data: any) => {
        // updatedData = JSON.parse(type);
        field.onChange(data);
        return console.log.bind(console, data);
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

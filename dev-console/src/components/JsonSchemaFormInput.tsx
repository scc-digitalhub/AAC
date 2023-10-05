import { useInput, InputProps } from 'react-admin';
// import Form from '@rjsf/mui';
import validator from '@rjsf/validator-ajv8';
import { RJSFSchema, UiSchema } from '@rjsf/utils';
import { Theme as MuiTheme } from '@rjsf/mui';
import { withTheme } from '@rjsf/core';

export const JSONSchemaFormInput = (props: JSONSchemaFormatInputProps) => {
    const { schema, uiSchema, onBlur, onChange } = props;

    const {
        field,
        fieldState: { isTouched, error },
        formState: { isSubmitted },
    } = useInput({
        onChange,
        onBlur,
        ...props,
    });

    const update = (data: any) => {
        field.onChange(data);
    };

    const Form = withTheme(MuiTheme);

    return (
        <Form
            schema={schema}
            uiSchema={uiSchema}
            formData={field.value}
            validator={validator}
            onChange={(e: any) => update(e.formData)}
            omitExtraData={true}
        >
            <></>
        </Form>
    );
};

export type JSONSchemaFormatInputProps = InputProps & {
    schema: RJSFSchema;
    uiSchema: UiSchema;
};

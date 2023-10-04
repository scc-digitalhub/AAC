import { useInput, InputProps } from 'react-admin';
import Form from '@rjsf/mui';
import validator from '@rjsf/validator-ajv8';

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

    return (
        <>
            <Form
                schema={schema}
                uiSchema={uiSchema}
                formData={field.value}
                validator={validator}
                onChange={(e: any) => update(e.formData)}
                omitExtraData={true}
            >
                <div>
                    <button hidden type="submit"></button>
                </div>
            </Form>
        </>
    );
};

export type JSONSchemaFormatInputProps = InputProps & {
    schema?: any;
    uiSchema?: any;
};

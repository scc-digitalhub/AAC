import {
    useInput,
    useTranslate,
    useTranslateLabel,
    InputProps,
} from 'react-admin';
import validator from '@rjsf/validator-ajv8';
import React from 'react';
import { RJSFSchema, UiSchema, GenericObjectType } from '@rjsf/utils';
import { Form } from '@rjsf/mui';

export const JsonSchemaFormInput = (props: JSONSchemaFormatInputProps) => {
    const {
        schema,
        uiSchema = {},
        label,
        helperText,
        resource,
        source,
        onBlur,
        onChange,
    } = props;
    const {
        field,
        fieldState: { isTouched, error },
        formState: { isSubmitted },
    } = useInput({
        onChange,
        onBlur,
        ...props,
    });
    const translate = useTranslate();
    const translateLabel = useTranslateLabel();

    const update = (data: any) => {
        field.onChange(data);
    };

    const rjschema: RJSFSchema =
        typeof schema === 'string'
            ? JSON.parse(schema)
            : (schema as RJSFSchema);

    const ruischema: UiSchema =
        typeof uiSchema === 'string'
            ? JSON.parse(uiSchema)
            : (uiSchema as UiSchema);

    //auto-add values from translation to uiSchema if missing
    const ui: GenericObjectType = ruischema as GenericObjectType;
    if (label && !('ui:title' in ui)) {
        ui['ui:title'] =
            typeof label === 'string'
                ? translate(label)
                : typeof label === 'boolean'
                ? translate(source)
                : '';
    }
    if (helperText && !('ui:description' in ui)) {
        ui['ui:description'] =
            typeof helperText === 'string' ? translate(helperText) : '';
    }

    //auto-enrich schema with titles from key when missing
    if (rjschema && 'properties' in rjschema) {
        for (const k in rjschema.properties) {
            const p: GenericObjectType = rjschema.properties[
                k
            ] as GenericObjectType;
            if (!('title' in p)) {
                p.title = k;
            }
            if (ui) {
                if (!(k in ui)) {
                    ui[k] = {};
                }

                if (!('ui:title' in ui[k])) {
                    //auto generate key and translate
                    ui[k]['ui:title'] = translateLabel({
                        source: source + '.' + k,
                        resource: resource,
                    });
                } else {
                    //translate user-provided
                    ui[k]['ui:title'] = translate(ui[k]['ui:title']);
                }
            }
        }
    }

    return (
        <Form
            tagName={'div'}
            schema={rjschema}
            uiSchema={ruischema}
            formData={field.value}
            validator={validator}
            onChange={(e: any) => update(e.formData)}
            omitExtraData={true}
            liveValidate={true}
            showErrorList={false}
        >
            <></>
        </Form>
    );
};

export type JSONSchemaFormatInputProps = InputProps & {
    schema: RJSFSchema | object | string;
    uiSchema?: UiSchema | object | string;
};

export default JsonSchemaFormInput;

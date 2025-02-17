import AceEditor from 'react-ace';

import 'ace-builds/src-noconflict/mode-html';
import 'ace-builds/src-noconflict/theme-github';

import { Fragment } from 'react';
import React from 'react';
import { useInput, InputProps, Labeled, InputHelperText } from 'react-admin';

// const ace = require('ace-builds/src-noconflict/ace');

export const TemplatesEditorInput = (props: TemplatesEditorProps) => {
    const {
        field: fieldKey,
        theme = 'github',
        useWorker = false,
        format = data => data,
        parse = data => data,
        //pick base from jsDelivr by default
        basePath = 'https://cdn.jsdelivr.net/npm/ace-builds@',
        label,
        helperText,
        fullWidth = false,
        minLines = 5,
        maxLines = 25,
        width = '50vw',
        onBlur,
        onChange,
        resource,
        source,
        disabled = false,
        ...rest
    } = props;

    const {
        id,
        field,
        fieldState: { isTouched, error },
        formState: { isSubmitted },
        isRequired,
    } = useInput({
        resource,
        source,
        // Pass the event handlers to the hook but not the component as the field property already has them.
        // useInput will call the provided onChange and onBlur in addition to the default needed by react-hook-form.
        onChange,
        onBlur,
        disabled,
        ...rest,
    });

    const value = field ? parse(field.value[fieldKey] || '') : '';
    const onValueChange = (data: string) => {
        const newValue = { ...field.value };
        newValue[fieldKey] = format(data);
        field.onChange(newValue);
    };

    const labelProps = {
        fullWidth,
        isRequired,
        label,
        resource,
        source,
    };

    //TODO let users customize options
    const aceOptions = {
        useWorker: useWorker,
        showPrintMargin: false,
        readOnly: disabled,
        minLines,
        maxLines,
    };

    return (
        <Fragment>
            <Labeled {...labelProps} id={id}>
                <AceEditor
                    mode={disabled ? 'text' : 'html'}
                    value={value}
                    onChange={onValueChange}
                    theme={theme}
                    wrapEnabled
                    width={fullWidth ? '100%' : width}
                    setOptions={aceOptions}
                />
            </Labeled>
            <InputHelperText
                touched={isTouched || isSubmitted}
                error={error?.message}
                helperText={helperText}
            />
        </Fragment>
    );
};

export type TemplatesEditorProps = InputProps & {
    field: string;
    format?: (string) => any | null;
    parse?: (any) => string | null;
    //use a worker for syntax check
    //disabled by default, loads from external cdn
    useWorker?: boolean;
    basePath?: string;
    fullWidth?: boolean;
    width?: string;
    minLines?: number;
    maxLines?: number;
    theme?: 'github';
};



// passo stesso source in IdpEdit
// se abilito e valore attuale non e' definito, setto default altrimento lascio stare
// se disabilito lo rimuovo
import { CheckBox } from '@mui/icons-material';
import Checkbox from '@mui/material/Checkbox';
import React, { Fragment, useState } from 'react';
import {
    useTranslate,
    Toolbar,
    SaveButton,
    Button,
    useInput,
    Labeled,
    InputHelperText,
    InputProps,
} from 'react-admin';
import { useNavigate } from 'react-router';

export const CheckBoxInput = (props: CheckBoxInputProps) => {
    // const [valueChecked,setValueChecked]=useState<boolean>(false)
    const {
        defaultFunction = null,
        label,
        helperText,
        fullWidth = false,
        width = '50vw',
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
    const [valueChecked,setValueChecked]=useState<boolean>(field.value?true:false);

    // console.log('CheckBox: defaultValue && !field?.value ? btoa(defaultValue) : field?.value ||',defaultValue && !field?.value ? btoa(defaultValue) : field?.value ||'')
    //  const valueCode = field?.value?.base64!=null ? atob(field?.value?.base64) : atob(defaultValue && !field?.value ? btoa(defaultValue) : field?.value ||'');    const valueChecked = field.value;
    // const valueChecked = field.value?true:false;
    // setValueChecked(field.value?true:false);
    const labelProps = {
        fullWidth,
        isRequired,
        label,
        resource,
        source,
    };
    const onCodeChange = (data: any) => {
        // const encodedValue = btoa(data);
        // field.onChange({ value:data, base64: encodedValue });
        // se abilito e valore attuale non e' definito, setto default altrimento lascio stare
        // se disabilito lo rimuovo
        if (data.currentTarget.checked)
        {
            setValueChecked(true);
            const encodedValue = btoa(defaultFunction);
            field.onChange({ value:defaultFunction, base64: encodedValue });}
            else {setValueChecked(false);
            const encodedValue = btoa('');
            field.onChange({ value:'', base64: encodedValue });
        }
    };
    // import workers (disabled by default)
    // NOTE: this should match *exactly* the included ace version
    // ace.config.set('basePath', basePath + ace.version + '/src-noconflict/');

    return (
        <Fragment>
            <Labeled {...labelProps} id={id}>
                <Checkbox
                    // {...field}
                    checked={valueChecked}
                    onChange={onCodeChange}
                    // width={fullWidth ? '100%' : width}
                    // setOptions={aceOptions}
                    // value={valueCode}
                    // onChange={onCodeChange}
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

export type CheckBoxInputProps = InputProps & {
     fullWidth?: boolean;
    width?: string;
    defaultFunction?: any;
};

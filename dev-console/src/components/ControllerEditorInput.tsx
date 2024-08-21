import { FieldTitle, isRequired, useInput } from 'react-admin';
import { AceEditorInput, AceInputProps } from './AceEditorInput';
import { Checkbox, FormControlLabel } from '@mui/material';
import { useState } from 'react';

export const ControlledEditorInput = (
    props: AceInputProps & { disabledValue?: any }
) => {
    const {
        resource,
        source,
        defaultValue = null,
        disabledValue = null,
        isRequired,
        ...rest
    } = props;
    const { field } = useInput({
        resource,
        source,
    });
    const checked = field?.value;
    const [disabled, setDisabled] = useState<boolean>(!checked);
    const handleChange = (event: React.ChangeEvent<HTMLInputElement>) => {
        const enabled = event.target.checked;
        if (enabled) {
            if (
                defaultValue &&
                (field.value === undefined ||
                    field.value === null ||
                    field.value === '')
            ) {
                //set default on enabling
                field.onChange(defaultValue);
            }
        } else {
            //set disabled value
            field.onChange(disabledValue);
        }
        setDisabled(!enabled);
    };

    return (
        <>
            <FormControlLabel
                control={
                    <Checkbox
                        defaultChecked={checked}
                        onChange={handleChange}
                    />
                }
                label={
                    <FieldTitle
                        label="action.enable_disable"
                        source={source}
                        resource={resource}
                        isRequired={isRequired}
                    />
                }
            />
            <AceEditorInput
                source={source}
                resource={resource}
                isRequired={isRequired}
                disabled={disabled}
                {...rest}
            />
        </>
    );
};

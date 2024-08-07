import { Stack, Avatar, TypographyOwnProps } from '@mui/material';
import { grey } from '@mui/material/colors';
import { FieldProps, TextField, useRecordContext } from 'react-admin';
import { isValidElement, ReactElement } from 'react';
import get from 'lodash/get';

export const NameField = (props: NameFieldProps) => {
    const { text, secondaryText, tertiaryText, icon } = props;
    const record = useRecordContext(props);
    const displayText = typeof text === 'string' ? get(record, text) : '';

    return (
        <Stack direction={'row'} columnGap={2} py={1}>
            <Avatar sx={{ mt: 1, backgroundColor: grey[200] }}>
                {icon && isValidElement(icon)
                    ? icon
                    : displayText.substring(0, 2)}
            </Avatar>
            <Stack>
                <OptionalTextField text={text} variant="h6" color="primary" />

                {secondaryText && (
                    <OptionalTextField text={secondaryText} variant="body2" />
                )}

                {tertiaryText && (
                    <OptionalTextField text={tertiaryText} variant="body2" />
                )}
            </Stack>
        </Stack>
    );
};

export const OptionalTextField = (
    props: {
        text: string | ReactElement;
    } & Pick<TypographyOwnProps, 'variant' | 'color'>
) => {
    const { text, variant, color } = props;
    if (!text) return <></>;

    return typeof text === 'string' ? (
        <TextField source={text} variant={variant} color={color} />
    ) : isValidElement(text) ? (
        text
    ) : (
        <></>
    );
};

export interface NameFieldProps extends FieldProps {
    //text is either a field name or an element
    text: string | ReactElement;
    //text is either a field name or an element
    secondaryText?: string | ReactElement;
    //text is either a field name or an element
    tertiaryText?: string | ReactElement;
    icon?: ReactElement;
}

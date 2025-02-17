import { Stack, Typography } from '@mui/material';
import get from 'lodash/get';
import { MouseEvent, useEffect, useState } from 'react';

import {
    TextFieldProps,
    useRecordContext,
    sanitizeFieldRestProps,
    useNotify,
    IconButtonWithTooltip,
    useTranslate,
} from 'react-admin';
import ContentCopyIcon from '@mui/icons-material/ContentCopy';
import VisibilityOffIcon from '@mui/icons-material/VisibilityOff';
import { grey } from '@mui/material/colors';

export const PasswordField = <
    RecordType extends Record<string, any> = Record<string, any>
>(
    props: TextFieldProps<RecordType> & {
        copy?: boolean;
        format?: (value: any) => any;
    }
) => {
    const {
        className,
        source,
        emptyText,
        copy = true,
        format = v => '*****',
        label,
        children,
        ...rest
    } = props;
    const record = useRecordContext(props);
    const notify = useNotify();
    const translate = useTranslate();

    const value = get(record, source) || null;
    const [displayValue, setDisplayValue] = useState(format(value));

    useEffect(() => {
        setDisplayValue(format(value));
    }, [value]);

    const displayLabel =
        label && typeof label === 'string'
            ? translate(label)
            : source && typeof source === 'string'
            ? translate(source)
            : translate('content');

    return (
        <Stack direction={'row'} columnGap={0}>
            <Typography
                component="span"
                variant="body2"
                className={className}
                {...sanitizeFieldRestProps(rest)}
                sx={{ background: grey[200], px: 1, my: 1 }}
                fontFamily={'monospace'}
            >
                {displayValue}
            </Typography>

            <IconButtonWithTooltip
                label="action.show_hide"
                onClick={(event: MouseEvent<HTMLElement>) => {
                    event.stopPropagation();

                    //toggle display
                    setDisplayValue(
                        value != displayValue ? value : format(value)
                    );
                }}
            >
                <VisibilityOffIcon fontSize="small" />
            </IconButtonWithTooltip>

            {copy && (
                <IconButtonWithTooltip
                    label="action.click_to_copy"
                    onClick={(event: MouseEvent<HTMLElement>) => {
                        event.stopPropagation();
                        if (value) {
                            navigator.clipboard.writeText(value);
                            notify('message.content_copied_x', {
                                messageArgs: { x: displayLabel },
                            });
                        }
                    }}
                >
                    <ContentCopyIcon fontSize="small" />
                </IconButtonWithTooltip>
            )}
            {children && children}
        </Stack>
    );
};

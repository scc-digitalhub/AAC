import { IconButton, Typography } from '@mui/material';
import get from 'lodash/get';
import {  MouseEvent } from 'react';

import {
    TextFieldProps,
    useRecordContext,
    sanitizeFieldRestProps,
    useNotify,
} from 'react-admin';
import ContentCopyIcon from '@mui/icons-material/ContentCopy';

export const IdField = <
    RecordType extends Record<string, any> = Record<string, any>
>(
    props: TextFieldProps<RecordType>
) => {
    const { className, source, emptyText, ...rest } = props;
    const record = useRecordContext(props);
    const value = get(record, source)?.toString();
    if (!value) return <>{source}</>;
    const notify = useNotify();
    return (
        <>
            <Typography
                component="span"
                variant="body2"
                className={className}
                {...sanitizeFieldRestProps(rest)}
                sx={{ background: 'lightgray', padding: '2px' }}
                fontFamily={'monospace'}
            >
                {value}
            </Typography>
            <IconButton
                onClick={(event: MouseEvent<HTMLElement>) => {
                    event.stopPropagation();
                    if (value) {
                        navigator.clipboard.writeText(value);
                        notify('content-copied');
                    }
                }}
            >
                <ContentCopyIcon fontSize="small" />
            </IconButton>
        </>
    );
};

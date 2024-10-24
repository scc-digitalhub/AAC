import {
    Typography,
    Paper,
    Container,
    Box,
    IconButton,
    Stack,
} from '@mui/material';
import { isValidElement, ReactElement } from 'react';
import {
    IconButtonWithTooltip,
    sanitizeFieldRestProps,
    useNotify,
} from 'react-admin';
import { MouseEvent } from 'react';
import ContentCopyIcon from '@mui/icons-material/ContentCopy';
import { grey } from '@mui/material/colors';

export const PageTitle = (props: PageTitleProps) => {
    const { text, secondaryText, copy, icon } = props;
    const notify = useNotify();

    return (
        <Box marginBottom={2}>
            <Stack direction={'row'} columnGap={2}>
                {icon && isValidElement(icon) ? icon : ''}
                <Box>
                    {text && isValidElement(text) ? (
                        text
                    ) : (
                        <Typography
                            variant="h4"
                            sx={{ pt: 0, pb: 1, textAlign: 'left' }}
                        >
                            {text}
                        </Typography>
                    )}
                    {secondaryText && (
                        <>
                            <Typography
                                component="span"
                                variant="h6"
                                sx={{
                                    p: '1px 5px',
                                    background: copy ? grey[200] : '',
                                    textAlign: 'left',
                                }}
                            >
                                {secondaryText}
                            </Typography>
                            {copy && (
                                <IconButtonWithTooltip
                                    label="action.click_to_copy"
                                    onClick={(
                                        event: MouseEvent<HTMLElement>
                                    ) => {
                                        event.stopPropagation();
                                        if (secondaryText) {
                                            navigator.clipboard.writeText(
                                                secondaryText
                                            );
                                            notify('message.content_copied');
                                        }
                                    }}
                                >
                                    <ContentCopyIcon
                                        fontSize="small"
                                        color="primary"
                                    />
                                </IconButtonWithTooltip>
                            )}
                        </>
                    )}
                </Box>
            </Stack>
        </Box>
    );
};

export interface PageTitleProps {
    text: string | ReactElement;
    secondaryText?: any;
    copy?: boolean;
    icon?: ReactElement;
}

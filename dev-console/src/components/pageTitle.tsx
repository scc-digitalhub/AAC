import { Typography, Paper, Container, Box, IconButton } from '@mui/material';
import { isValidElement, ReactElement } from 'react';
import { sanitizeFieldRestProps,useNotify } from 'react-admin';
import {  MouseEvent } from 'react';
import ContentCopyIcon from '@mui/icons-material/ContentCopy';

export const PageTitle = (props: PageTitleProps) => {
    const { text, secondaryText, icon } = props;
    const notify = useNotify();

    return (
            <Box>
                {icon && isValidElement(icon) ? icon : ''}
                <Typography
                    variant="h4"
                    sx={{ pt: 0, pb: 1, textAlign: 'left' }}
                >
                    {text}
                </Typography>
                {secondaryText && (
                     <>
                     <Typography
                         component="span"
                         variant="h6"
                         sx={{ pt: 0, pb: 1,  background: 'lightgray',padding: '2px',textAlign: 'left' }}
                         
                     >
                       
                         {secondaryText}
                     </Typography>
                     <IconButton
                         onClick={(event: MouseEvent<HTMLElement>) => {
                             event.stopPropagation();
                             if (secondaryText) {
                                 navigator.clipboard.writeText(secondaryText);
                                 notify('content-copied');
                             }
                         }}
                     >
                         <ContentCopyIcon fontSize="small" />
                     </IconButton>
                 </>
                )}
            </Box>
    );
};

export interface PageTitleProps {
    text: string;
    secondaryText?: any;
    icon?: ReactElement;
}

import { Typography, Paper, Container, Box } from '@mui/material';
import { isValidElement, ReactElement } from 'react';

export const PageTitle = (props: PageTitleProps) => {
    const { text, secondaryText, icon } = props;

    return (
            <Box >
                {icon && isValidElement(icon) ? icon : ''}
                <Typography
                    variant="h4"
                    sx={{ pt: 0, pb: 1, textAlign: 'left' }}
                >
                    {text}
                </Typography>
                {secondaryText && (
                    <Typography
                        variant="h6"
                        sx={{ pt: 0, pb: 1, textAlign: 'left' }}
                    >
                        {secondaryText}
                    </Typography>
                )}
            </Box>
    );
};

export interface PageTitleProps {
    text: string;
    secondaryText?: string|number|undefined;
    icon?: ReactElement;
}

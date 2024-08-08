import { Box, Typography } from '@mui/material';
import { isValidElement, ReactElement } from 'react';
import { useTranslate } from 'react-admin';

export const SectionTitle = (props: SectionTitleProps) => {
    const translate = useTranslate();
    const { text, secondaryText } = props;

    return (
        <Box marginBottom={2}>
            <Typography variant="h5" sx={{ pt: 0, pb: 1, textAlign: 'left' }}>
                {translate(text || '')}
            </Typography>
            {secondaryText && typeof secondaryText === 'string' ? (
                <Typography
                    variant="h6"
                    sx={{ pt: 0, pb: 1, textAlign: 'left' }}
                >
                    {translate(secondaryText || '')}
                </Typography>
            ) : isValidElement(secondaryText) ? (
                secondaryText
            ) : (
                ''
            )}
        </Box>
    );
};

export interface SectionTitleProps {
    text: string;
    secondaryText?: string | number | ReactElement | undefined;
}

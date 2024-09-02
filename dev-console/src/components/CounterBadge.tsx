import { Grid, Paper, Typography, useTheme } from '@mui/material';
import { fontSize, fontWeight, textAlign } from '@mui/system';

export const CounterBadge = (props: {
    value: number | undefined;
    variant?: 'elevation' | 'outlined';
    elevation?: number;
    color?: string;
    backgroundColor?: string;
    size?: 'small' | 'medium' | 'large';
    className?: string;
}) => {
    const {
        value,
        variant = 'elevation',
        elevation = 0,
        color: colorProps,
        backgroundColor: backgroundColorProps,
        size = 'medium',
        className,
    } = props;
    const theme = useTheme();
    const color = colorProps || theme.palette.primary.main;
    const backgroundColor =
        backgroundColorProps || theme.palette.background.default;

    const wh = (function () {
        switch (size) {
            case 'small':
                return { width: '1.6em', height: '1.6em' };
            case 'medium':
                return { width: '3.2em', height: '3.2em' };
            case 'large':
                return { width: '5em', height: '5em' };
        }
    })();

    const textProps = (function () {
        switch (size) {
            case 'small':
                return { fontSize: '0.52em', fontWeight: 'medium' };
            case 'medium':
                return { fontSize: '1.22em' };
            case 'large':
                return { fontSize: '1.78em' };
        }
    })();

    const sxProps = {
        lineHeight: '100%',
        aspectRatio: 1,
        display: 'inline-grid',
        placeItems: 'center',
        padding: '.5em',
        borderRadius: '50%',
        boxSizing: 'border-box',
        textAlign: 'center',
        backgroundColor,
        color,
        ...wh,
    };

    return (
        // <Grid container justifyContent="center">
        //     <Grid item md={4} zeroMinWidth key={1} textAlign={'center'}>
        <Paper
            elevation={elevation}
            variant={variant}
            sx={sxProps}
            className={className}
        >
            <Typography variant="h6" sx={{ textAlign: 'center', ...textProps }}>
                {value}
            </Typography>
        </Paper>
        //     </Grid>
        // </Grid>
    );
};

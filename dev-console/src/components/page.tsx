import { styled } from '@mui/material/styles';
import { Container, Paper, PaperProps } from '@mui/material';

export const Page = (props: PaperProps) => {
    const { children, className, elevation = 0, ...rest } = props;

    return (
        <Container maxWidth={false} sx={{ pb: 2 }}>
            <StyledPaper className={className} elevation={elevation} {...rest}>
                {children}
            </StyledPaper>
        </Container>
    );
};

const StyledPaper = styled(Paper, {
    name: 'ContainerPaper',
})(({ theme }) => ({
    padding: theme.spacing(3),
    // flex: 1,
    // display: 'flex',
    // backgroundColor: theme.palette.grey[100],
}));
